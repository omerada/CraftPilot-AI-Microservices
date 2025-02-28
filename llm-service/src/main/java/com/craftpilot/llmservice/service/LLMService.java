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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.time.Duration;
import org.springframework.http.HttpStatusCode;  // Yeni import eklendi

@Service
@Slf4j
@RequiredArgsConstructor
public class LLMService {
    
    private final WebClient openRouterWebClient;

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
        request.setModel("anthropic/claude-2");
        return callOpenRouter("/v1/chat/completions", request)
            .map(response -> mapToAIResponse(response, request));
    }

    // OpenRouter görsel modeli desteklemediği için alternatif servis kullanımı
    public Mono<AIResponse> processImageGeneration(AIRequest request) {
        return Mono.error(new UnsupportedOperationException(
            "Image generation is not supported by OpenRouter. Please use Stability AI or DALL-E API directly."));
    }

    private Mono<Map<String, Object>> callOpenRouter(String endpoint, AIRequest request) {
        log.info("OpenRouter API çağrısı yapılıyor - Endpoint: {}, Request: {}", endpoint, request);
        Map<String, Object> requestBody = createRequestBody(request);
        
        // URL path düzeltmesi
        String correctedEndpoint = "/v1/chat/completions";  // Sabit endpoint kullan

        return openRouterWebClient.post()
            .uri(correctedEndpoint)
            .bodyValue(requestBody)
            .retrieve()
            .onStatus(HttpStatusCode::is4xxClientError, response ->
                response.bodyToMono(String.class)
                    .flatMap(error -> {
                        log.error("Client error: {}", error);
                        return Mono.error(new APIException("Client error: " + error));
                    }))
            .onStatus(HttpStatusCode::is5xxServerError, response ->
                response.bodyToMono(String.class)
                    .flatMap(error -> {
                        log.error("Server error: {}", error);
                        return Mono.error(new APIException("Server error: " + error));
                    }))
            .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
            .timeout(Duration.ofSeconds(30))
            .doOnNext(response -> log.debug("OpenRouter raw response: {}", response))
            .doOnError(error -> log.error("API call error: ", error))
            .onErrorResume(error -> {
                log.error("API request failed", error);
                return Mono.error(new APIException("API request failed: " + error.getMessage()));
            });
    }

    private Map<String, Object> createRequestBody(AIRequest request) {
        Map<String, Object> body = new HashMap<>();
        
        // Model kontrolü
        body.put("model", request.getModel() != null ? 
            request.getModel() : "google/gemini-pro");
        
        // Messages formatını düzelt
        List<Map<String, Object>> messages = new ArrayList<>();
        Map<String, Object> message = new HashMap<>();
        message.put("role", "user");uter formatına uygun şekilde düzenleme
        message.put("content", request.getPrompt());
        messages.add(message);
        body.put("messages", messages);
        
        // Opsiyonel parametrelermatta oluştur
        if (request.getTemperature() != null) {ntList = new ArrayList<>();
            body.put("temperature", request.getTemperature());Map<String, Object> textContent = new HashMap<>();
        }text");
        if (request.getMaxTokens() != null) {pt());
            body.put("max_tokens", request.getMaxTokens());
        }
        
        log.debug("Created request body: {}", body);
        return body;ody.put("messages", messages);
    }

    // Özel exception sınıfıgetTemperature() != null) {
    public static class APIException extends RuntimeException {       body.put("temperature", request.getTemperature());
        public APIException(String message) {        }
            super(message);kens() != null) {
        }
    }

    private AIResponse mapToAIResponse(Map<String, Object> openRouterResponse, AIRequest request) {og.debug("Created request body: {}", body);
        log.debug("OpenRouter yanıtı haritalanıyor: {}", openRouterResponse);   return body;
            }
        String responseText = extractResponseText(openRouterResponse);
        if (responseText == null || responseText.trim().isEmpty()) {
            log.warn("OpenRouter'dan boş yanıt alındı");ic static class APIException extends RuntimeException {
            throw new RuntimeException("AI servisinden boş yanıt alındı");
        }

        AIResponse response = AIResponse.success(
            responseText,
            request.getModel(),    private AIResponse mapToAIResponse(Map<String, Object> openRouterResponse, AIRequest request) {
            extractTokenCount(openRouterResponse),r: {}", openRouterResponse);
            request.getRequestId()
        );xtractResponseText(openRouterResponse);
rim().isEmpty()) {
        log.debug("Haritalanan yanıt: {}", response);an boş yanıt alındı");
        return response;  throw new RuntimeException("AI servisinden boş yanıt alındı");
    }        }

    private String extractResponseText(Map<String, Object> response) {nse = AIResponse.success(
        try {       responseText,
            log.debug("Yanıt içeriği: {}", response);            request.getModel(),
            if (response.containsKey("choices")) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");equest.getRequestId()
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
                                    .findFirst()t<Map<String, Object>>) response.get("choices");
                                    .orElse(null);
                            } else {
                                return (String) message.get("content");age")) {
                            }e = (Map<String, Object>) choice.get("message");
                        }containsKey("content")) {
                    } {
                }   List<Map<String, Object>> contents = (List<Map<String, Object>>) message.get("content");
            }       return contents.stream()
            throw new RuntimeException("Geçersiz API yanıt formatı");               .filter(content -> "text".equals(content.get("type")))
        } catch (Exception e) {                   .map(content -> (String) content.get("text"))
            log.error("Yanıt işlenirken hata oluştu: ", e);                       .findFirst()
            throw new RuntimeException("AI yanıtı işlenemedi", e);
        }lse {
    }("content");

    private Integer extractTokenCount(Map<String, Object> response) {               }
        Map<String, Object> usage = (Map<String, Object>) response.getOrDefault("usage", Map.of());               }
        return ((Number) usage.getOrDefault("total_tokens", 0)).intValue();                }
    }
}
    }

    private Integer extractTokenCount(Map<String, Object> response) {
        Map<String, Object> usage = (Map<String, Object>) response.getOrDefault("usage", Map.of());
        return ((Number) usage.getOrDefault("total_tokens", 0)).intValue();
    }
}
