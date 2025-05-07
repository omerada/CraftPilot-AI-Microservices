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
@RequiredArgsConstructor
@Slf4j
public class UserMemoryClient {
    private final WebClient.Builder webClientBuilder;

    @Value("${user-memory.service.url:http://user-memory-service:8067}")
    private String userMemoryServiceUrl;

    @Value("${user-memory.request.timeout.seconds:15}")
    private int requestTimeoutSeconds;

    @CircuitBreaker(name = "userMemoryService", fallbackMethod = "addMemoryEntryFallback")
    @Retry(name = "userMemoryService")
    public Mono<String> addMemoryEntry(ExtractedUserInfo extractedInfo) {
        // userId kontrolü ekleyelim
        if (extractedInfo == null || extractedInfo.getUserId() == null || extractedInfo.getUserId().trim().isEmpty()) {
            log.warn("User ID is null or empty. Cannot send memory entry to user-memory-service.");
            return Mono.just("ERROR-NULL-USER-ID");
        }
        
        log.info("Sending memory entry to user-memory-service for user: {}", extractedInfo.getUserId());
        
        MemoryEntryRequest request = new MemoryEntryRequest();
        request.setContent(extractedInfo.getInformation());
        request.setSource(extractedInfo.getSource() != null ? extractedInfo.getSource() : "Varsayılan kayıt");
        request.setContext(extractedInfo.getContext());
        request.setTimestamp(LocalDateTime.ofInstant(extractedInfo.getTimestamp(), ZoneId.systemDefault()));
        
        // userId'yi metadata içine ekleyelim (request nesnesine doğrudan ekleyemiyoruz)
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("extractionMethod", "ai-llm");
        metadata.put("messageLength", extractedInfo.getInformation().length());
        metadata.put("extractionTimestamp", System.currentTimeMillis());
        metadata.put("userId", extractedInfo.getUserId()); // userId'yi metadata içinde taşıyalım
        
        request.setMetadata(metadata);
        
        // Önemlik seviyesi - şimdilik sabit 2.0
        request.setImportance(2.0);
        
        log.debug("Memory entry request details: content={}, source={}", 
            extractedInfo.getInformation(), request.getSource());
        
        // AI yanıtını detaylı şekilde logla
        log.info("AI extracted information for user {}: {}", 
            extractedInfo.getUserId(), 
            extractedInfo.getInformation().length() > 100 ? 
                extractedInfo.getInformation().substring(0, 100) + "..." : 
                extractedInfo.getInformation());
        
        return webClientBuilder.build()
                .post()
                .uri(userMemoryServiceUrl + "/memories/entries")
                .header("X-User-Id", extractedInfo.getUserId())
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(BodyInserters.fromValue(request))
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(requestTimeoutSeconds))
                .doOnSubscribe(s -> log.info("API isteği gönderiliyor: /memories/entries, userId: {}", extractedInfo.getUserId()))
                .doOnSuccess(result -> {
                    log.info("Memory entry successfully added for user: {}", extractedInfo.getUserId());
                    log.debug("Stored AI extraction content: {}", extractedInfo.getInformation().length() > 50 ? 
                        extractedInfo.getInformation().substring(0, 50) + "..." : 
                        extractedInfo.getInformation());
                })
                .doOnError(e -> {
                    log.error("Error sending memory entry to user-memory-service: {}", e.getMessage());
                    logClientError(e, extractedInfo.getUserId());
                })
                .retryWhen(reactor.util.retry.Retry.fixedDelay(3, Duration.ofMillis(500))
                    .filter(ex -> !(ex instanceof WebClientResponseException.BadRequest)));
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
        
        return webClientBuilder.build()
                .get()
                .uri(userMemoryServiceUrl + "/memories/{userId}", userId)
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
