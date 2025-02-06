package com.craftpilot.llmservice.repository;

import com.craftpilot.llmservice.model.AIResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Slf4j
@Repository
@RequiredArgsConstructor
public class CacheRepository {
    private final ReactiveRedisTemplate<String, AIResponse> redisTemplate;
    private static final Duration CACHE_DURATION = Duration.ofHours(24);

    public Mono<AIResponse> get(String key) {
        return redisTemplate.opsForValue().get(key)
                .doOnSuccess(result -> {
                    if (result != null) {
                        log.debug("Cache hit for key: {}", key);
                    } else {
                        log.debug("Cache miss for key: {}", key);
                    }
                })
                .doOnError(error -> log.error("Error retrieving from cache: {}", error.getMessage()));
    }

    public Mono<Boolean> set(String key, AIResponse response) {
        return redisTemplate.opsForValue()
                .set(key, response, CACHE_DURATION)
                .doOnSuccess(result -> log.debug("Cached response for key: {}", key))
                .doOnError(error -> log.error("Error caching response: {}", error.getMessage()));
    }

    public Mono<Boolean> delete(String key) {
        return redisTemplate.opsForValue().delete(key)
                .doOnSuccess(result -> log.debug("Deleted cache for key: {}", key))
                .doOnError(error -> log.error("Error deleting cache: {}", error.getMessage()));
    }

    public String generateKey(String model, String content) {
        return String.format("%s:%s", model, content.hashCode());
    }
} 