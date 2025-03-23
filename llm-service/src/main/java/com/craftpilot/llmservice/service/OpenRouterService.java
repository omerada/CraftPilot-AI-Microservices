package com.craftpilot.llmservice.service;

import com.craftpilot.llmservice.config.OpenRouterConfig;
import com.craftpilot.llmservice.model.AIRequest;
import com.craftpilot.llmservice.model.AIResponse;
import com.craftpilot.llmservice.event.AIEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreaker;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "kafka.enabled", havingValue = "true", matchIfMissing = false)
public class OpenRouterService {
    private final WebClient webClient;
    private final OpenRouterConfig config;
    private final ReactiveCircuitBreaker circuitBreaker;
    private final KafkaTemplate<String, AIEvent> kafkaTemplate;

    @Value("${kafka.topics.ai-events:ai-events}")
    private String aiEventsTopic;

    public Mono<AIResponse> processRequest(AIRequest request) {
        if (request.getRequestId() == null) {
            request.setRequestId(UUID.randomUUID().toString());
        }
        
        return executeWithCircuitBreaker(callOpenRouterAPI(request));
    }

    private Mono<AIResponse> callOpenRouterAPI(AIRequest request) {
        return webClient.post()
                .uri(config.getBaseUrl() + "/chat/completions")
                .header("Authorization", "Bearer " + config.getApiKey())
                .bodyValue(createRequestBody(request))
                .retrieve()
                .bodyToMono(AIResponse.class)
                .map(response -> {
                    sendAIEvent(request, response, "AI_COMPLETION");
                    return response;
                });
    }

    private Mono<AIResponse> handleError(Throwable throwable, AIRequest request) {
        log.error("Error calling OpenRouter API for request {}: {}", 
                request.getRequestId(), throwable.getMessage());
        
        // Updated to use the new AIResponse structure
        AIResponse errorResponse = AIResponse.builder()
                .response("Service temporarily unavailable") // Changed from .error() to .response()
                .model(request.getModel() != null ? request.getModel() : config.getDefaultModel())
                .success(false) // Added this to indicate error status
                .build();

        sendAIEvent(request, null, throwable.getMessage());
        return Mono.just(errorResponse);
    }

    private void sendAIEvent(AIRequest request, AIResponse response, String error) {
        AIEvent event = error != null ? 
            AIEvent.error(request, error) :
            AIEvent.fromRequest(request, response, "AI_COMPLETION");

        kafkaTemplate.send(aiEventsTopic, request.getRequestId(), event)
            .whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to send AI event for request: {}", request.getRequestId(), ex);
                } else {
                    log.debug("AI event sent successfully for request: {}", request.getRequestId());
                }
            });
    }

    private Map<String, Object> createRequestBody(AIRequest request) {
        return Map.of(
            "model", request.getModel() != null ? request.getModel() : config.getDefaultModel(),
            "messages", request.getMessages(),
            "max_tokens", request.getMaxTokens() != null ? request.getMaxTokens() : config.getMaxTokens(),
            "temperature", request.getTemperature() != null ? request.getTemperature() : config.getTemperature()
        );
    }

    private void publishEvent(AIEvent event) {
        try {
            kafkaTemplate.send(aiEventsTopic, event.getRequestId(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish event to kafka: {}", ex.getMessage());
                    } else {
                        log.debug("Event published successfully: {}", event);
                    }
                });
        } catch (Exception e) {
            log.error("Error publishing event: {}", e.getMessage());
        }
    }

    public <T> Mono<T> executeWithCircuitBreaker(Mono<T> operation) {
        return circuitBreaker.run(operation, throwable -> 
            Mono.error(new RuntimeException("Service is not available", throwable)));
    }
}