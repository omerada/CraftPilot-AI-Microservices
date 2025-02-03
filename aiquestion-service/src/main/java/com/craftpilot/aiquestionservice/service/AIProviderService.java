package com.craftpilot.aiquestionservice.service;

import com.craftpilot.aiquestionservice.model.AIModel;
import com.craftpilot.aiquestionservice.model.Question;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator; 
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value; 
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class AIProviderService {
    
    private final WebClient openaiWebClient;
    private final WebClient anthropicWebClient;
    private final WebClient geminiWebClient;
    
    @Value("${ai.openai.api-key}")
    private String openaiApiKey;
    
    @Value("${ai.anthropic.api-key}")
    private String anthropicApiKey;

    @Value("${ai.google.api-key}")
    private String googleApiKey;

    private final Map<String, CircuitBreaker> circuitBreakers = new ConcurrentHashMap<>();

    public AIProviderService(
            @Qualifier("openaiWebClient") WebClient openaiWebClient,
            @Qualifier("anthropicWebClient") WebClient anthropicWebClient,
            @Qualifier("geminiWebClient") WebClient geminiWebClient) {
        this.openaiWebClient = openaiWebClient;
        this.anthropicWebClient = anthropicWebClient;
        this.geminiWebClient = geminiWebClient;
    }

    private CircuitBreaker getCircuitBreaker(String modelId) {
        return circuitBreakers.computeIfAbsent(modelId, key -> {
            CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                    .failureRateThreshold(50)
                    .waitDurationInOpenState(Duration.ofSeconds(60))
                    .permittedNumberOfCallsInHalfOpenState(10)
                    .slidingWindowSize(100)
                    .build();
            return CircuitBreaker.of(key, config);
        });
    }

    public Mono<String> processQuestion(Question question, AIModel model) {
        CircuitBreaker circuitBreaker = getCircuitBreaker(model.getId());
        return processQuestionInternal(question, model)
                .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
                .doOnSuccess(response -> log.info("Successfully processed question with model ID: {}", model.getId()))
                .doOnError(error -> log.error("Error processing question with model ID {}: {}", model.getId(), error.getMessage()));
    }

    private Mono<String> processQuestionInternal(Question question, AIModel model) {
        return switch (model.getType()) {
            case GPT_4, GPT_4_TURBO, GPT_3_5_TURBO -> processOpenAIQuestion(question, model);
            case CLAUDE_3_OPUS, CLAUDE_3_SONNET, CLAUDE_3_HAIKU -> processAnthropicQuestion(question, model);
            case GEMINI_PRO, GEMINI_ULTRA -> processGeminiQuestion(question, model);
            case PALM_2, LLAMA_2, MISTRAL, CUSTOM -> throw new UnsupportedOperationException("Model type not supported: " + model.getType());
        };
    }

    private Mono<String> processOpenAIQuestion(Question question, AIModel model) {
        return openaiWebClient.post()
            .uri("/v1/chat/completions")
            .header("Authorization", "Bearer " + openaiApiKey)
            .bodyValue(createOpenAIRequest(question, model))
            .retrieve()
            .bodyToMono(String.class)
            .doOnError(error -> log.error("OpenAI API error: {}", error.getMessage()));
    }

    private Map<String, Object> createOpenAIRequest(Question question, AIModel model) {
        return Map.of(
            "model", model.getType().toString().toLowerCase(),
            "messages", List.of(
                Map.of(
                    "role", "user",
                    "content", question.getContent()
                )
            )
        );
    }

    private Mono<String> processAnthropicQuestion(Question question, AIModel model) {
        return anthropicWebClient.post()
            .uri("/v1/messages")
            .header("x-api-key", anthropicApiKey)
            .header("anthropic-version", "2024-01-01")
            .bodyValue(createAnthropicRequest(question, model))
            .retrieve()
            .bodyToMono(String.class)
            .doOnError(error -> log.error("Anthropic API error: {}", error.getMessage()));
    }

    private Map<String, Object> createAnthropicRequest(Question question, AIModel model) {
        return Map.of(
            "model", model.getType().toString().toLowerCase(),
            "messages", List.of(
                Map.of(
                    "role", "user",
                    "content", question.getContent()
                )
            ),
            "max_tokens", 1000
        );
    }

    private Mono<String> processGeminiQuestion(Question question, AIModel model) {
        return geminiWebClient.post()
            .uri("/v1/models/" + model.getType().toString().toLowerCase() + ":generateContent")
            .header("x-goog-api-key", googleApiKey)
            .bodyValue(createGeminiRequest(question))
            .retrieve()
            .bodyToMono(String.class)
            .doOnError(error -> log.error("Gemini API error: {}", error.getMessage()));
    }

    private Map<String, Object> createGeminiRequest(Question question) {
        return Map.of(
            "contents", List.of(
                Map.of(
                    "parts", List.of(
                        Map.of("text", question.getContent())
                    )
                )
            )
        );
    }
} 