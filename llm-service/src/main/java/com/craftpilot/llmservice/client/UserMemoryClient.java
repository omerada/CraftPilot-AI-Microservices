package com.craftpilot.llmservice.client;

import com.craftpilot.llmservice.dto.ExtractedUserInfo;
import com.craftpilot.llmservice.model.UserMemory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserMemoryClient {

    private final WebClient.Builder webClientBuilder;

    @Value("${user-memory.service.url:http://user-memory-service:8067}")
    private String userMemoryServiceUrl;

    @Value("${user-memory.request.timeout.seconds:15}")
    private int requestTimeoutSeconds;

    @CircuitBreaker(name = "userMemoryService", fallbackMethod = "addMemoryEntryFallback")
    public Mono<String> addMemoryEntry(ExtractedUserInfo extractedInfo) {
        log.info("Sending memory entry to user-memory-service for user: {}", extractedInfo.getUserId());
        
        MemoryEntryRequest request = new MemoryEntryRequest();
        request.setContent(extractedInfo.getInformation());
        request.setSource(extractedInfo.getSource());
        request.setContext(extractedInfo.getContext());
        request.setTimestamp(LocalDateTime.ofInstant(extractedInfo.getTimestamp(), ZoneId.systemDefault()));
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("extractionMethod", "ai-llm");
        metadata.put("messageLength", extractedInfo.getInformation().length());
        request.setMetadata(metadata);
        
        // Önemlik seviyesi - şimdilik sabit 2.0
        request.setImportance(2.0);
        
        return webClientBuilder.build()
                .post()
                .uri(userMemoryServiceUrl + "/memories/entries")
                .header("X-User-Id", extractedInfo.getUserId())
                .header("Content-Type", "application/json")
                .body(BodyInserters.fromValue(request))
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(requestTimeoutSeconds))
                .doOnSubscribe(s -> log.debug("API request başlatıldı: /memories/entries"))
                .doOnSuccess(result -> log.info("Memory entry successfully added for user: {}", extractedInfo.getUserId()))
                .doOnError(e -> logClientError(e, extractedInfo.getUserId()));
    }

    // Circuit breaker için fallback metodu
    private Mono<String> addMemoryEntryFallback(ExtractedUserInfo extractedInfo, Throwable e) {
        log.warn("Circuit breaker triggered for memory storage: userId={}, error={}", 
                extractedInfo.getUserId(), e.getMessage());
        
        // Hata ayrıntılarını kaydet
        if (e instanceof TimeoutException) {
            log.error("Memory service timeout for user {}: {}", extractedInfo.getUserId(), e.getMessage());
        } else if (e instanceof WebClientResponseException) {
            WebClientResponseException wcre = (WebClientResponseException) e;
            log.error("Memory service HTTP error for user {}: {} - {}", 
                    extractedInfo.getUserId(), wcre.getStatusCode(), wcre.getResponseBodyAsString());
        }
        
        // Burada retry queue veya cache'e kaydedilebilir
        return Mono.just("FALLBACK-RESPONSE-MEMORY-STORAGE-DEFERRED");
    }

    private void logClientError(Throwable e, String userId) {
        if (e instanceof WebClientResponseException) {
            WebClientResponseException wcre = (WebClientResponseException) e;
            log.error("User memory service error for user {}: Status={}, Response={}", 
                    userId, wcre.getStatusCode(), wcre.getResponseBodyAsString());
        } else {
            log.error("Failed to communicate with user memory service for user {}: {}", 
                    userId, e.getMessage());
        }
    }

    public Mono<UserMemory> getUserMemory(String userId) {
        log.info("Fetching memory for user: {}", userId);
        
        return webClientBuilder.build()
                .get()
                .uri(userMemoryServiceUrl + "/user-memory/{userId}", userId)
                .retrieve()
                .bodyToMono(UserMemory.class)
                .doOnSuccess(memory -> log.info("Memory fetched successfully for user {}: {} entries", 
                        userId, memory != null && memory.getMemory() != null ? memory.getMemory().size() : 0))
                .doOnError(e -> log.error("Error fetching memory for user {}: {}", userId, e.getMessage()))
                .onErrorResume(e -> {
                    log.error("Failed to retrieve memory. Returning empty memory. Error: {}", e.getMessage());
                    return Mono.just(new UserMemory());
                });
    }
}
