package com.craftpilot.llmservice.client;

import com.craftpilot.llmservice.dto.ExtractedUserInfo;
import com.craftpilot.llmservice.dto.MemoryEntryRequest;
import com.craftpilot.llmservice.model.UserMemory;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

@Component
@Slf4j
public class UserMemoryClient {

    private final WebClient webClient;
    
    @Value("${user-memory.non-meaningful-strings:Kullanıcı mesaj gönderdi,Mesajdan bilgi çıkarılamadı,Kullanıcıdan bilgi çıkarılamadı}")
    private String nonMeaningfulStrings;

    public UserMemoryClient(@Value("${user-memory-service.url:http://user-memory-service:8080}") String baseUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    @CircuitBreaker(name = "userMemoryService", fallbackMethod = "addMemoryEntryFallback")
    @Retry(name = "userMemoryService")
    public Mono<String> addMemoryEntry(ExtractedUserInfo extractedInfo) {
        if (extractedInfo == null) {
            log.warn("Cannot add null memory entry");
            return Mono.just("NULL-ENTRY-SKIPPED");
        }
        
        String info = extractedInfo.getInformation();
        
        // Anlamsız bilgileri filtrele
        if (info == null || info.isEmpty() || isNonMeaningfulInformation(info)) {
            log.info("Skipping storage for non-meaningful information: {}", info);
            return Mono.just("SKIPPED-NON-MEANINGFUL-INFO");
        }
        
        log.debug("Sending memory entry to user-memory-service: {}", extractedInfo);
        
        return webClient.post()
                .uri("/api/memory")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(extractedInfo)
                .retrieve()
                .bodyToMono(String.class)
                .doOnSuccess(response -> log.debug("Memory entry added successfully: {}", response))
                .doOnError(error -> log.error("Error adding memory entry: {}", error.getMessage()));
    }
    
    // Anlamsız bilgileri daha esnek bir şekilde kontrol et
    private boolean isNonMeaningfulInformation(String info) {
        if (info == null || info.isEmpty()) {
            return true;
        }
        
        // Yapılandırılabilir anlamsız string listesi
        String[] nonMeaningfulList = nonMeaningfulStrings.split(",");
        for (String nonMeaningful : nonMeaningfulList) {
            if (info.trim().equalsIgnoreCase(nonMeaningful.trim())) {
                return true;
            }
        }
        
        // Çok kısa ve herhangi bir spesifik bilgi içermeyen metinleri filtrele
        if (info.length() < 10 && !info.contains(":")) {
            return true;
        }
        
        return false;
    }

    // Circuit breaker için fallback metodu
    private Mono<String> addMemoryEntryFallback(ExtractedUserInfo extractedInfo, Throwable e) {
        // Eğer extractedInfo null ise güvenli bir şekilde işlem yapalım
        if (extractedInfo == null) {
            log.warn("Circuit breaker triggered for memory storage but extractedInfo is null");
            return Mono.just("FALLBACK-RESPONSE-NULL-USER-INFO");
        }
        
        // userId null ise güvenli bir şekilde işlem yapalım
        String userId = extractedInfo.getUserId();
        if (userId == null || userId.isEmpty()) {
            log.warn("Circuit breaker triggered for memory storage with null/empty userId, error: {}", e.getMessage());
            return Mono.just("FALLBACK-RESPONSE-NULL-USER-ID");
        }
        
        log.warn("Circuit breaker triggered for memory storage: userId={}, error={}", 
            userId, e.getMessage());
        
        // Hata ayrıntılarını kaydet
        if (e instanceof TimeoutException) {
            log.error("Memory service timeout for user {}: {}", userId, e.getMessage());
        } else if (e instanceof WebClientResponseException) {
            WebClientResponseException wcre = (WebClientResponseException) e;
            log.error("Memory service HTTP error for user {}: {} - {}", 
                    userId, wcre.getStatusCode(), wcre.getResponseBodyAsString());
        } else {
            log.error("Unexpected error type for user {}: {} (Type: {})", 
                    userId, e.getMessage(), e.getClass().getName());
        }
        
        // Cache'e veya retry queue'ya kaydetme işlemi buraya eklenebilir
        
        return Mono.just("FALLBACK-RESPONSE-MEMORY-STORAGE-DEFERRED");
    }

    private void logClientError(Throwable e, String userId) {
        // userId null güvenlik kontrolü
        if (userId == null || userId.isEmpty()) {
            userId = "UNKNOWN_USER";
        }
        
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
        // userId null güvenlik kontrolü
        if (userId == null || userId.trim().isEmpty()) {
            log.warn("Cannot fetch memory for null or empty user ID");
            return Mono.just(new UserMemory()); // Boş bir bellek döndür
        }
        
        log.info("Fetching memory for user: {}", userId);
        
        return webClient.get()
                .uri("/memories/{userId}", userId)
                .header("X-User-Id", userId)
                .retrieve()
                .bodyToMono(UserMemory.class)
                .doOnSubscribe(s -> log.info("API isteği gönderiliyor: /memories/{}", userId))
                .doOnSuccess(memory -> log.info("Memory fetched successfully for user {}: {} entries", 
                        userId, memory != null && memory.getMemory() != null ? memory.getMemory().size() : 0))
                .doOnError(e -> {
                    if (e instanceof WebClientResponseException) {
                        WebClientResponseException wcre = (WebClientResponseException) e;
                        log.error("Memory service HTTP error for user {}: Status={}, Body={}", 
                                userId, wcre.getStatusCode(), wcre.getResponseBodyAsString());
                    } else {
                        log.error("Error fetching memory for user {}: {} (Type: {})", 
                                userId, e.getMessage(), e.getClass().getName());
                    }
                })
                .onErrorResume(e -> {
                    log.error("Failed to retrieve memory. Returning empty memory. Error: {}", e.getMessage());
                    return Mono.just(new UserMemory());
                });
    }
}
