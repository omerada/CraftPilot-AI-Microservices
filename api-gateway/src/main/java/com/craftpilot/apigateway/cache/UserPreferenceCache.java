package com.craftpilot.apigateway.cache;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Slf4j
@Component
public class UserPreferenceCache {
    private final WebClient webClient;
    private final AsyncLoadingCache<String, String> languageCache;
    private final Executor cacheExecutor;

    public UserPreferenceCache(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
                .baseUrl("http://user-service")
                .build();
        
        this.cacheExecutor = Executors.newFixedThreadPool(2);
        
        this.languageCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofHours(1))
            .maximumSize(10000)
            .executor(cacheExecutor)
            .buildAsync((key, executor) -> loadUserLanguageAsync(key));
    }
    
    public Mono<String> getUserLanguage(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return Mono.just("en");
        }
        
        return Mono.fromFuture(() -> languageCache.get(userId))
            .onErrorResume(e -> {
                log.warn("Dil tercihi alınamadı: {}, varsayılan 'en' kullanılıyor", e.getMessage());
                return Mono.just("en");
            });
    }
    
    private CompletableFuture<String> loadUserLanguageAsync(String userId) {
        return webClient.get()
            .uri("/users/{userId}/preferences", userId)
            .retrieve()
            .bodyToMono(Map.class)
            .map(preferences -> {
                Object languageObj = preferences.get("language");
                String language = languageObj != null ? languageObj.toString() : "en";
                log.debug("Kullanıcı {} için dil tercihi yüklendi: {}", userId, language);
                return language;
            })
            .onErrorResume(e -> {
                log.warn("Kullanıcı tercihi alınamadı {}: {}", userId, e.getMessage());
                return Mono.just("en");
            })
            .toFuture();
    }
    
    public void invalidate(String userId) {
        languageCache.synchronous().invalidate(userId);
        log.debug("Kullanıcı {} için dil önbelleği temizlendi", userId);
    }
    
    public void refreshCache(String userId) {
        languageCache.synchronous().refresh(userId);
        log.debug("Kullanıcı {} için dil önbelleği yenileniyor", userId);
    }
}
