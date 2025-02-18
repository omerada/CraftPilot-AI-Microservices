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
        log.debug("Request body: {}", requestBody);

        return openRouterWebClient.post()
            .uri(endpoint)
            .bodyValue(requestBody)
            .retrieve()
            .onStatus(status -> status.is4xxClientError(), response ->
                response.bodyToMono(String.class)
                    .flatMap(error -> {
                        log.error("Client error: {}", error);
                        return Mono.error(new RuntimeException("API client error: " + error));
                    }))
            .onStatus(status -> status.is5xxServerError(), response ->
                response.bodyToMono(String.class)
                    .flatMap(error -> {
                        log.error("Server error: {}", error);
                        return Mono.error(new RuntimeException("API server error: " + error));
                    }))
            .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
            .timeout(Duration.ofSeconds(30))
            .doOnNext(response -> log.debug("OpenRouter raw response: {}", response))
            .doOnError(error -> log.error("API call error: ", error))
            .onErrorResume(error -> Mono.error(new RuntimeException("API request failed: " + error.getMessage())));
    }

    private Map<String, Object> createRequestBody(AIRequest request) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", request.getModel());
        
        // Code completion için özel mesaj formatı
        if ("CODE".equals(request.getRequestType())) {
            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of(
                "role", "system",
                "content", "You are a code assistant. Write clean and well-documented code."
            ));
            messages.add(Map.of(
                "role", "user",
                "content", request.getPrompt()
            ));
            body.put("messages", messages);
        } else if (request.getMessages() != null && !request.getMessages().isEmpty()) {
            body.put("messages", request.getMessages());
        } else {
            body.put("prompt", request.getPrompt());
        }

        body.put("max_tokens", request.getMaxTokens());
        body.put("temperature", request.getTemperature());
        body.put("stream", false);
        
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
            log.debug("Extracting response from: {}", response);
            if (response.containsKey("choices")) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
                if (!choices.isEmpty()) {
                    Map<String, Object> choice = choices.get(0);
                    if (choice.containsKey("message")) {
                        Map<String, Object> message = (Map<String, Object>) choice.get("message");
                        return (String) message.get("content");
                    } else if (choice.containsKey("text")) {
                        return (String) choice.get("text");
                    }
                }
            }
            throw new RuntimeException("Invalid response format from OpenRouter");
        } catch (Exception e) {
            log.error("Error extracting response text: ", e);
            throw new RuntimeException("Failed to process AI response", e);
        }
    }

    private Integer extractTokenCount(Map<String, Object> response) {
        Map<String, Object> usage = (Map<String, Object>) response.getOrDefault("usage", Map.of());
        return ((Number) usage.getOrDefault("total_tokens", 0)).intValue();
    }
}
