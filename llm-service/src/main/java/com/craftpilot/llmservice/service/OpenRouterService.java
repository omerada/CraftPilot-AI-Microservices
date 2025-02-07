package com.craftpilot.llmservice.service;

import com.craftpilot.llmservice.config.OpenRouterConfig;
import com.craftpilot.llmservice.model.AIRequest;
import com.craftpilot.llmservice.model.AIResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreaker;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenRouterService {
    private final WebClient webClient;
    private final OpenRouterConfig config;
    private final ReactiveCircuitBreaker circuitBreaker;

    public Mono<AIResponse> processRequest(AIRequest request) {
        if (request.getRequestId() == null) {
            request.setRequestId(UUID.randomUUID().toString());
        }
        
        return circuitBreaker.run(
            callOpenRouterAPI(request),
            throwable -> handleError(throwable, request)
        );
    }

    private Mono<AIResponse> callOpenRouterAPI(AIRequest request) {
        return webClient.post()
                .uri(config.getBaseUrl() + "/chat/completions")
                .header("Authorization", "Bearer " + config.getApiKey())
                .bodyValue(createRequestBody(request))
                .retrieve()
                .bodyToMono(AIResponse.class)
                .map(response -> {
                    response.setRequestId(request.getRequestId());
                    return response;
                });
    }

    private Mono<AIResponse> handleError(Throwable throwable, AIRequest request) {
        log.error("Error calling OpenRouter API for request {}: {}", 
                request.getRequestId(), throwable.getMessage());
        return Mono.just(AIResponse.builder()
                .error("Service temporarily unavailable")
                .model(request.getModel() != null ? request.getModel() : config.getDefaultModel())
                .requestId(request.getRequestId())
                .build());
    }

    private Map<String, Object> createRequestBody(AIRequest request) {
        return Map.of(
            "model", request.getModel() != null ? request.getModel() : config.getDefaultModel(),
            "messages", request.getMessages(),
            "max_tokens", request.getMaxTokens() != null ? request.getMaxTokens() : config.getMaxTokens(),
            "temperature", request.getTemperature() != null ? request.getTemperature() : config.getTemperature()
        );
    }
} 