package com.craftpilot.llmservice.service;

import com.craftpilot.llmservice.model.AIRequest;
import com.craftpilot.llmservice.model.AIResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        return openRouterWebClient.post()
            .uri(endpoint)
            .bodyValue(createRequestBody(request))
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
            .doOnError(error -> log.error("OpenRouter API error: ", error));
    }

    private Map<String, Object> createRequestBody(AIRequest request) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", request.getModel());
        
        if (request.getMessages() != null) {
            body.put("messages", request.getMessages());
        } else if (request.getPrompt() != null) {
            body.put("prompt", request.getPrompt());
        }

        body.put("max_tokens", request.getMaxTokens());
        body.put("temperature", request.getTemperature());
        
        return body;
    }

    private AIResponse mapToAIResponse(Map<String, Object> openRouterResponse, AIRequest request) {
        log.debug("OpenRouter yanıtı: {}", openRouterResponse); // Debug log ekleyelim
        String responseText = extractResponseText(openRouterResponse);
        if (responseText.isEmpty()) {
            log.error("Boş yanıt alındı");
            throw new RuntimeException("AI servisi boş yanıt döndü");
        }

        return AIResponse.builder()
            .requestId(request.getRequestId())
            .model(request.getModel())
            .response(responseText)
            .tokensUsed(extractTokenCount(openRouterResponse))
            .processingTime(System.currentTimeMillis())
            .status("SUCCESS")
            .build();
    }

    private String extractResponseText(Map<String, Object> response) {
        // OpenRouter response yapısına göre yanıt çıkarma
        if (response.containsKey("choices")) {
            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
            if (!choices.isEmpty()) {
                Map<String, Object> choice = choices.get(0);
                if (choice.containsKey("message")) {
                    return ((Map<String, String>) choice.get("message")).get("content");
                } else if (choice.containsKey("text")) {
                    return (String) choice.get("text");
                }
            }
        }
        return "";
    }

    private Integer extractTokenCount(Map<String, Object> response) {
        Map<String, Object> usage = (Map<String, Object>) response.getOrDefault("usage", Map.of());
        return ((Number) usage.getOrDefault("total_tokens", 0)).intValue();
    }
}
