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

@Component
@RequiredArgsConstructor
@Slf4j
public class UserMemoryClient {

    private final WebClient.Builder webClientBuilder;

    @Value("${user-memory.service.url:http://user-memory-service:8067}")
    private String userMemoryServiceUrl;

    @Value("${user-memory.request.timeout.seconds:5}")
    private int requestTimeoutSeconds;

    @CircuitBreaker(name = "userMemoryService", fallbackMethod = "addMemoryEntryFallback")
    public Mono<String> addMemoryEntry(ExtractedUserInfo extractedInfo) {
        log.info("Sending memory entry to user-memory-service: userId={}, info={}", 
                extractedInfo.getUserId(), extractedInfo.getInformation());
        
        if (extractedInfo.getTimestamp() == null) {
            log.debug("Setting timestamp on extracted info before sending to memory service");
            extractedInfo.setTimestamp(java.time.Instant.now());
        }
        
        return webClientBuilder.build()
                .post()
                .uri(userMemoryServiceUrl + "/user-memory/{userId}/entries", extractedInfo.getUserId())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(extractedInfo)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(requestTimeoutSeconds))
                .doOnSubscribe(s -> log.debug("Making request to user-memory-service API"))
                .doOnSuccess(response -> log.info("Successfully added memory for user {}: {}", 
                        extractedInfo.getUserId(), response))
                .doOnError(e -> logClientError(e, extractedInfo.getUserId()));
    }

    // Circuit breaker için fallback metodu
    private Mono<String> addMemoryEntryFallback(ExtractedUserInfo extractedInfo, Throwable e) {
        log.warn("Circuit breaker triggered for memory storage: {}", e.getMessage());
        // Burada fallback stratejisini uygulayabilirsiniz
        // Örneğin: Yerel bir cache'e kaydet, daha sonra retry et, veya başka bir yedek servise gönder
        return Mono.just("FALLBACK-RESPONSE-MEMORY-STORAGE-DEFERRED");
    }

    private void logClientError(Throwable e, String userId) {
        if (e instanceof WebClientResponseException) {
            WebClientResponseException wcre = (WebClientResponseException) e;
            log.error("Error adding memory for user {}. Status: {}, Response: {}", 
                    userId, wcre.getStatusCode(), wcre.getResponseBodyAsString());
        } else {
            log.error("Error adding memory for user {}: {}", userId, e.getMessage());
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
