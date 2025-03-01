package com.craftpilot.llmservice.service;

import com.craftpilot.llmservice.model.AIRequest;
import com.craftpilot.llmservice.model.AIResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.time.Duration;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.reactive.function.client.ClientResponse;
import com.craftpilot.llmservice.exception.APIException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Objects;

@Service
@Slf4j
@RequiredArgsConstructor
public class LLMService {
    private final WebClient openRouterWebClient; 
    private final ObjectMapper objectMapper;

    public Mono<AIResponse> processTextCompletion(AIRequest request) {
        return callOpenRouter("/v1/completions", request)
            .map(response -> mapToAIResponse(response, request));
    }

    public Mono<AIResponse> processChatCompletion(AIRequest request) {
        return callOpenRouter("/v1/chat/completions", request)
            .map(response -> mapToAIResponse(response, request));
    }

    public Mono<AIResponse> processCodeCompletion(AIRequest request) {
        // Code completion için özel model seçimi
        // request.setModel("anthropic/claude-2");
        return callOpenRouter("/v1/chat/completions", request)
            .map(response -> mapToAIResponse(response, request));
    }

    // OpenRouter görsel modeli desteklemediği için alternatif servis kullanımı
    public Mono<AIResponse> processImageGeneration(AIRequest request) {
        return Mono.error(new UnsupportedOperationException(
            "Image generation is not supported by OpenRouter. Please use Stability AI or DALL-E API directly."));
    }

    public Flux<AIResponse> streamChatCompletion(AIRequest request) {
        Map<String, Object> requestBody = createRequestBody(request);
        requestBody.put("stream", true);
        
        return openRouterWebClient.post()
            .uri("/v1/chat/completions")
            .bodyValue(requestBody)
            .retrieve()
            .bodyToFlux(String.class)
            .map(chunk -> {
                if (chunk.startsWith("data: ")) {
                    chunk = chunk.substring(6);
                }
                try {
                    Map<String, Object> response = objectMapper.readValue(chunk, new TypeReference<Map<String, Object>>() {});
                    return AIResponse.builder()
                        .requestId(request.getRequestId())
                        .response(extractChunkContent(response))
                        .model(request.getModel())
                        .status("STREAMING")
                        .success(true)
                        .build();
                } catch (Exception e) {
                    log.error("Error parsing chunk: {}", e.getMessage());
                    return null;
                }
            })
            .filter(Objects::nonNull);
    }

    private Mono<Map<String, Object>> callOpenRouter(String endpoint, AIRequest request) {
        return openRouterWebClient.post()
            .uri("/v1/" + endpoint)
            .bodyValue(createRequestBody(request))
            .retrieve()
            .onStatus(HttpStatusCode::isError, this::handleError)
            .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
            .timeout(Duration.ofSeconds(30));
    }

    private Mono<? extends Throwable> handleError(ClientResponse response) {
        return response.bodyToMono(String.class)
            .flatMap(error -> {
                String message = String.format("OpenRouter API Error [%s]: %s", 
                    response.statusCode(), error);
                log.error(message);
                return Mono.error(new APIException(message));
            });
    }

    private Map<String, Object> createRequestBody(AIRequest request) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", request.getModel());
        
        List<Map<String, Object>> messages = new ArrayList<>();
        Map<String, Object> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        
        List<Map<String, Object>> content = new ArrayList<>();
        Map<String, Object> textContent = new HashMap<>();
        textContent.put("type", "text");
        textContent.put("text", request.getPrompt());
        content.add(textContent);
        
        userMessage.put("content", content);
        messages.add(userMessage);
        
        body.put("messages", messages);
        body.put("max_tokens", request.getMaxTokens());
        body.put("temperature", request.getTemperature());
        
        log.debug("Oluşturulan request body: {}", body);
        return body;
    }

    private AIResponse mapToAIResponse(Map<String, Object> openRouterResponse, AIRequest request) {
        log.debug("OpenRouter yanıtı haritalanıyor: {}", openRouterResponse);
        
        String responseText = extractResponseText(openRouterResponse);
        if (responseText == null || responseText.trim().isEmpty()) {
            log.warn("OpenRouter'dan boş yanıt alındı");
            throw new RuntimeException("AI servisinden boş yanıt alındı");
        }

        AIResponse response = AIResponse.success(
            responseText,
            request.getModel(),
            extractTokenCount(openRouterResponse),
            request.getRequestId()
        );

        log.debug("Haritalanan yanıt: {}", response);
        return response;
    }

    private String extractResponseText(Map<String, Object> response) {
        try {
            log.debug("Yanıt içeriği: {}", response);
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
                                    .orElse(null);
                            } else {
                                return (String) message.get("content");
                            }
                        }
                    }
                }
            }
            throw new RuntimeException("Geçersiz API yanıt formatı");
        } catch (Exception e) {
            log.error("Yanıt işlenirken hata oluştu: ", e);
            throw new RuntimeException("AI yanıtı işlenemedi", e);
        }
    }

    private Integer extractTokenCount(Map<String, Object> response) {
        Map<String, Object> usage = (Map<String, Object>) response.getOrDefault("usage", Map.of());
        return ((Number) usage.getOrDefault("total_tokens", 0)).intValue();
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
}
