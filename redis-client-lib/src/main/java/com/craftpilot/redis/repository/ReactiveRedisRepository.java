package com.craftpilot.redis.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Slf4j
@RequiredArgsConstructor
public class ReactiveRedisRepository {

    private final ReactiveRedisTemplate<String, Object> redisTemplate;

    public <T> Mono<T> get(String key, Class<T> classType) {
        return redisTemplate.opsForValue().get(key)
                .cast(classType)
                .doOnSuccess(result -> {
                    if (result != null) {
                        log.debug("Retrieved from cache for key: {}", key);
                    } else {
                        log.debug("Cache miss for key: {}", key);
                    }
                })
                .doOnError(error -> log.error("Error retrieving from cache: {}", error.getMessage()));
    }

    public <T> Mono<Boolean> set(String key, T value) {
        return redisTemplate.opsForValue().set(key, value)
                .doOnSuccess(result -> log.debug("Stored in cache for key: {}", key))
                .doOnError(error -> log.error("Error storing in cache: {}", error.getMessage()));
    }

    public <T> Mono<Boolean> set(String key, T value, Duration ttl) {
        return redisTemplate.opsForValue().set(key, value, ttl)
                .doOnSuccess(result -> log.debug("Stored in cache with TTL for key: {}", key))
                .doOnError(error -> log.error("Error storing in cache with TTL: {}", error.getMessage()));
    }

    public Mono<Boolean> delete(String key) {
        return redisTemplate.opsForValue().delete(key)
                .doOnSuccess(result -> log.debug("Deleted cache for key: {}", key))
                .doOnError(error -> log.error("Error deleting cache: {}", error.getMessage()));
    }
    
    public Mono<Boolean> hasKey(String key) {
        return redisTemplate.hasKey(key)
                .doOnSuccess(result -> log.debug("Checked existence for key: {}, exists: {}", key, result))
                .doOnError(error -> log.error("Error checking key existence: {}", error.getMessage()));
    }
    
    public String generateKey(String prefix, String content) {
        return String.format("%s:%s", prefix, content.hashCode());
    }
}
