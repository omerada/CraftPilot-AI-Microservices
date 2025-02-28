package com.craftpilot.llmservice.service;

import com.craftpilot.llmservice.model.AIRequest;
import com.craftpilot.llmservice.model.AIResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.time.Duration;
import java.util.Objects;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
@Slf4j
@RequiredArgsConstructor
public class LLMService {
    
    private final WebClient openRouterWebClient;

    public Mono<AIResponse> processTextCompletion(AIRequest request) {
        // Text completion için chat/completions endpointi kullanılacak
        return processChatCompletion(request);
    }

    public Mono<AIResponse> processChatCompletion(AIRequest request) {
        return callOpenRouter("chat/completions", request)
            .map(response -> mapToAIResponse(response, request));
    }

    public Mono<AIResponse> processCodeCompletion(AIRequest request) {
        request.setModel("anthropic/claude-2");
        return callOpenRouter("/v1/chat/completions", request)
            .map(response -> mapToAIResponse(response, request));
    }

    public Mono<AIResponse> processImageGeneration(AIRequest request) {
        return Mono.error(new UnsupportedOperationException(
            "Image generation is not supported by OpenRouter. Please use Stability AI or DALL-E API directly."));
    }

    public Flux<AIResponse> streamChatCompletion(AIRequest request) {
        Map<String, Object> requestBody = createRequestBody(request);
        requestBody.put("stream", true);  // Stream modunu aktifleştir
        
        return openRouterWebClient.post()
            .uri("/v1/chat/completions")
            .bodyValue(requestBody)
            .retrieve()
            .bodyToFlux(String.class)
            .filter(chunk -> !chunk.isEmpty())
            .map(chunk -> {
                // SSE formatındaki chunk'ı parse et
                if (chunk.startsWith("data: ")) {
                    chunk = chunk.substring(6);
                }
                if ("[DONE]".equals(chunk)) {
                    return null;
                }
                
                try {
                    Map<String, Object> response = parseChunk(chunk);
                    String content = extractChunkContent(response);
                    
                    return AIResponse.builder()
                        .requestId(request.getRequestId())
                        .response(content)
                        .model(request.getModel())
                        .status("STREAMING")
                        .success(true)
                        .build();
                } catch (Exception e) {
                    log.error("Chunk parse error: {}", e.getMessage());
                    return null;
                }
            })
            .filter(Objects::nonNull)
            .doOnComplete(() -> log.debug("Stream completed for request: {}", request.getRequestId()));
    }

    private Map<String, Object> parseChunk(String chunk) {
        try {
            return new ObjectMapper().readValue(chunk, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.error("Error parsing chunk: {}", chunk, e);
            throw new APIException("Chunk parse error: " + e.getMessage());
        }
    }

    private String extractChunkContent(Map<String, Object> response) {
        try {
            Map<String, Object> choice = ((List<Map<String, Object>>) response.get("choices")).get(0);
            Map<String, Object> delta = (Map<String, Object>) choice.get("delta");
            return (String) delta.get("content");
        } catch (Exception e) {
            log.error("Error extracting content from chunk: {}", response, e);
            return "";
        }
    }

    private Mono<Map<String, Object>> callOpenRouter(String endpoint, AIRequest request) {
        log.info("OpenRouter API çağrısı yapılıyor - Request: {}", request);
        Map<String, Object> requestBody = createRequestBody(request);
        
        return openRouterWebClient.post()
            .uri(uriBuilder -> uriBuilder.path("/v1/" + endpoint).build())
            .bodyValue(requestBody)
            .retrieve()
            .onStatus(HttpStatusCode::isError, response ->
                response.bodyToMono(String.class)
                    .flatMap(error -> {
                        log.error("API error: Status={}, Body={}", response.statusCode(), error);
                        return Mono.error(new APIException(String.format("OpenRouter API Error [%s]: %s", 
                            response.statusCode(), error)));
                    }))
            .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
            .timeout(Duration.ofSeconds(30))
            .doOnNext(response -> log.debug("OpenRouter response: {}", response))
            .onErrorResume(error -> {
                log.error("Request failed", error);
                return Mono.error(new APIException("OpenRouter request failed: " + error.getMessage()));
            });
    }

    private Map<String, Object> createRequestBody(AIRequest request) {
        Map<String, Object> body = new HashMap<>();
        
        // Model seçimi ve kontrolü
        String model = request.getModel();
        if (model == null || model.trim().isEmpty()) {
            model = "google/gemini-2.0-flash-lite-001";  // Default model
        }
        body.put("model", model);
        
        // Messages array formatı
        List<Map<String, Object>> messages = new ArrayList<>();
        Map<String, Object> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", request.getPrompt());
        messages.add(message);
        body.put("messages", messages);
        
        // Parametreler
        body.put("temperature", request.getTemperature() != null ? 
            request.getTemperature() : 0.7);
        body.put("max_tokens", request.getMaxTokens() != null ? 
            request.getMaxTokens() : 2000);
        
        log.debug("Created request body: {}", body);
        return body;
    }

    public static class APIException extends RuntimeException {
        public APIException(String message) {
            super(message);
        }
    }

    private AIResponse mapToAIResponse(Map<String, Object> openRouterResponse, AIRequest request) {
        log.debug("OpenRouter yanıtı haritalanıyor: {}", openRouterResponse);
        
        String responseText = extractResponseText(openRouterResponse);
        if (responseText == null || responseText.trim().isEmpty()) {
            log.warn("OpenRouter'dan boş yanıt alındı");
            throw new RuntimeException("AI servisinden boş yanıt alındı");
        }

        return AIResponse.success(
            responseText,
            request.getModel(),
            extractTokenCount(openRouterResponse),
            request.getRequestId()
        );
    }

    private String extractResponseText(Map<String, Object> response) {
        try {
            log.debug("Extracting response from: {}", response);
            if (response.containsKey("choices")) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
                if (!choices.isEmpty()) {
                    Map<String, Object> choice = choices.get(0);
                    if (choice.containsKey("message")) {
                        Map<String, Object> message = (Map<String, Object>) choice.get("message");
                        if (message.containsKey("content")) {
                            if (message.get("content") instanceof List) {
                                List<Map<String, Object>> contents = (List<Map<String, Object>>) message.get("content");
                                return contents.stream()
                                    .filter(content -> "text".equals(content.get("type")))
                                    .map(content -> (String) content.get("text"))
                                    .findFirst()
                                    .orElse("");
                            } else {
                                return (String) message.get("content");
                            }
                        }
                    }
                }
            }
            throw new APIException("Geçersiz API yanıt formatı");
        } catch (Exception e) {
            log.error("Yanıt işlenirken hata oluştu: {}", e.getMessage(), e);
            throw new APIException("AI yanıtı işlenemedi: " + e.getMessage());
        }
    }

    private Integer extractTokenCount(Map<String, Object> response) {
        Map<String, Object> usage = (Map<String, Object>) response.getOrDefault("usage", Map.of());
        return ((Number) usage.getOrDefault("total_tokens", 0)).intValue();
    }
}
