package com.craftpilot.llmservice.client;

import com.craftpilot.llmservice.dto.ExtractedUserInfo;
import com.craftpilot.llmservice.model.UserMemory;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class UserMemoryClient {
    private final WebClient webClient;

    public UserMemoryClient(WebClient.Builder webClientBuilder, 
                           @Value("${service.user-memory.url:http://user-memory-service}") String baseUrl) {
        this.webClient = webClientBuilder
                .baseUrl(baseUrl)
                .build();
    }

    @CircuitBreaker(name = "userMemoryService", fallbackMethod = "getUserMemoryFallback")
    public Mono<UserMemory> getUserMemory(String userId) {
        return webClient.get()
                .uri("/user-memory/{userId}", userId)
                .retrieve()
                .bodyToMono(UserMemory.class)
                .doOnSuccess(memory -> log.info("Retrieved user memory for user: {}", userId))
                .doOnError(e -> log.error("Error retrieving user memory for user: {}", userId, e));
    }

    @CircuitBreaker(name = "userMemoryService", fallbackMethod = "addMemoryEntryFallback")
    public Mono<UserMemory> addMemoryEntry(ExtractedUserInfo info) {
        UserMemory.MemoryEntry entry = UserMemory.MemoryEntry.builder()
                .timestamp(info.getTimestamp())
                .information(info.getInformation())
                .context(info.getContext())
                .build();

        return webClient.post()
                .uri("/user-memory/{userId}/entry", info.getUserId())
                .bodyValue(entry)
                .retrieve()
                .bodyToMono(UserMemory.class)
                .doOnSuccess(memory -> log.info("Added memory entry for user: {}", info.getUserId()))
                .doOnError(e -> log.error("Error adding memory entry for user: {}", info.getUserId(), e));
    }

    // Fallback methods
    public Mono<UserMemory> getUserMemoryFallback(String userId, Exception e) {
        log.warn("Using fallback for getUserMemory: {}", e.getMessage());
        return Mono.just(UserMemory.builder().userId(userId).build());
    }

    public Mono<UserMemory> addMemoryEntryFallback(ExtractedUserInfo info, Exception e) {
        log.warn("Using fallback for addMemoryEntry: {}", e.getMessage());
        return Mono.just(UserMemory.builder().userId(info.getUserId()).build());
    }
}
