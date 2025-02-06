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
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenRouterService {
    private final WebClient webClient;
    private final OpenRouterConfig config; 
    private final ReactiveCircuitBreaker circuitBreaker;

    public Mono<AIResponse> processRequest(AIRequest request) {
        Map<String, Object> requestBody = buildRequestBody(request);
        
        return webClient.post()
                .uri(config.getBaseUrl() + "/chat/completions")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(AIResponse.class)
                .transform(it -> circuitBreaker.run(it, throwable -> handleError(throwable)))
                .retryWhen(Retry.backoff(config.getRetryAttempts(), Duration.ofMillis(config.getRetryDelay()))
                        .filter(throwable -> shouldRetry(throwable)))
                .doOnError(error -> log.error("Error processing request: {}", error.getMessage()))
                .doOnSuccess(response -> log.info("Successfully processed request"));
    }

    private Map<String, Object> buildRequestBody(AIRequest request) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", request.getModel() != null ? request.getModel() : config.getDefaultModel());
        body.put("messages", request.getMessages());
        body.put("max_tokens", request.getMaxTokens() != null ? request.getMaxTokens() : config.getMaxTokens());
        body.put("temperature", request.getTemperature() != null ? request.getTemperature() : config.getTemperature());
        return body;
    }

    private Mono<AIResponse> handleError(Throwable throwable) {
        log.error("Circuit breaker fallback triggered: {}", throwable.getMessage());
        return Mono.error(new RuntimeException("Service temporarily unavailable"));
    }

    private boolean shouldRetry(Throwable throwable) {
        return throwable instanceof Exception && 
               !(throwable instanceof IllegalArgumentException);
    }
} 