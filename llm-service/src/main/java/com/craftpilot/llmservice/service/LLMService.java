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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.time.Duration;

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
        request.setModel("anthropic/claude-2");
        return callOpenRouter("/v1/chat/completions", request)
            .map(response -> mapToAIResponse(response, request));
    }

    public Mono<AIResponse> processImageGeneration(AIRequest request) {
        return Mono.error(new UnsupportedOperationException(
            "Image generation is not supported by OpenRouter. Please use Stability AI or DALL-E API directly."));
    }

    private Mono<Map<String, Object>> callOpenRouter(String endpoint, AIRequest request) {
        log.info("OpenRouter API çağrısı yapılıyor - Endpoint: {}, Request: {}", endpoint, request);
        Map<String, Object> requestBody = createRequestBody(request);
        
        String correctedEndpoint = "/v1/chat/completions";

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
        
        body.put("model", request.getModel() != null ? 
            request.getModel() : "google/gemini-pro");
        
        List<Map<String, Object>> messages = new ArrayList<>();
        Map<String, Object> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", request.getPrompt());
        messages.add(message);
        body.put("messages", messages);
        
        if (request.getTemperature() != null) {
            body.put("temperature", request.getTemperature());
        }
        if (request.getMaxTokens() != null) {
            body.put("max_tokens", request.getMaxTokens());
        }
        
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
            if (response.containsKey("choices")) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
                if (!choices.isEmpty()) {
                    Map<String, Object> choice = choices.get(0);
                    if (choice.containsKey("message")) {
                        Map<String, Object> message = (Map<String, Object>) choice.get("message");
                        if (message.containsKey("content")) {
                            return (String) message.get("content");
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
}
