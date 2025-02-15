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
    private final KafkaTemplate<String, AIEvent> kafkaTemplate;

    @Value("${kafka.topics.ai-events}")
    private String aiEventsTopic;

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
                    sendAIEvent(request, response, "AI_COMPLETION");
                    return response;
                });
    }

    private Mono<AIResponse> handleError(Throwable throwable, AIRequest request) {
        log.error("Error calling OpenRouter API for request {}: {}", 
                request.getRequestId(), throwable.getMessage());
        
        AIResponse errorResponse = AIResponse.builder()
                .error("Service temporarily unavailable")
                .model(request.getModel() != null ? request.getModel() : config.getDefaultModel())
                .requestId(request.getRequestId())
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
}