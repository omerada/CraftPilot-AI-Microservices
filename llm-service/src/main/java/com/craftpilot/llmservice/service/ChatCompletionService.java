package com.craftpilot.llmservice.service;

import com.craftpilot.llmservice.config.OpenRouterProperties;
import com.craftpilot.llmservice.exception.APIException;
import com.craftpilot.llmservice.model.AIRequest;
import com.craftpilot.llmservice.model.AIResponse;
import com.craftpilot.llmservice.service.client.OpenRouterClient;
import com.craftpilot.llmservice.util.ResponseExtractor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

/**
 * Chat tamamlama işlemlerini yönetir
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatCompletionService {
    private final OpenRouterClient openRouterClient;
    private final ResponseExtractor responseExtractor;
    private final OpenRouterProperties properties;

    /**
     * AI isteğini işler ve tamamlanmış bir yanıt döndürür
     */
    public Mono<AIResponse> processChatCompletion(AIRequest request) {
        // Request validasyonu
        if (request.getRequestId() == null) {
            request.setRequestId(UUID.randomUUID().toString());
        }
        
        // Model belirtilmemişse varsayılan bir model ata
        if (request.getModel() == null || request.getModel().isEmpty()) {
            request.setModel(properties.getDefaultModel());
        }
         
        
        return openRouterClient.callOpenRouter("chat/completions", request) 
            .map(response -> mapToAIResponse(response, request))
            .timeout(Duration.ofSeconds(properties.getRequestTimeoutSeconds()))
            .doOnError(e -> log.error("Chat completion error: {}", e.getMessage(), e))
            .onErrorResume(e -> {
                log.error("Hata yakalandı: {}", e.getMessage());
                AIResponse errorResponse = AIResponse.builder()
                    .error("Request timeout")
                    .success(false)
                    .build();
                return Mono.just(errorResponse);
            });
    }
    
    /**
     * Kod tamamlama isteğini işler
     */
    public Mono<AIResponse> processCodeCompletion(AIRequest request) {
        request.setRequestType("CODE");
        return openRouterClient.callOpenRouter("chat/completions", request)
            .map(response -> mapToAIResponse(response, request));
    }
    
    /**
     * OpenRouter yanıtını AIResponse'a dönüştürür
     */
    private AIResponse mapToAIResponse(Map<String, Object> openRouterResponse, AIRequest request) { 
        String responseText = responseExtractor.extractResponseText(openRouterResponse);
        
        if (responseText == null || responseText.trim().isEmpty()) {
            log.warn("OpenRouter'dan boş yanıt alındı");
            throw new APIException("AI servisinden boş yanıt alındı");
        }
        
        AIResponse response = AIResponse.builder()
            .response(responseText)
            .model(request.getModel())
            .tokenCount(responseExtractor.extractTokenCount(openRouterResponse))
            .requestId(request.getRequestId())
            .success(true)
            .build();
             
        return response;
    }
}
