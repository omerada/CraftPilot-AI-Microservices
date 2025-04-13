package com.craftpilot.redis.service;

import com.craftpilot.redis.repository.ReactiveRedisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.function.Supplier;

@Slf4j
@RequiredArgsConstructor
public class CacheService {

    private final ReactiveRedisRepository redisRepository;
    private final Duration defaultTtl;

    public <T> Mono<T> getOrCache(String key, Class<T> classType, Supplier<Mono<T>> dataSupplier) {
        return getOrCache(key, classType, dataSupplier, defaultTtl);
    }

    public <T> Mono<T> getOrCache(String key, Class<T> classType, Supplier<Mono<T>> dataSupplier, Duration ttl) {
        return redisRepository.get(key, classType)
                .switchIfEmpty(
                        dataSupplier.get()
                                .flatMap(data -> 
                                    redisRepository.set(key, data, ttl)
                                            .thenReturn(data)
                                )
                );
    }

    public <T> Mono<Boolean> cacheData(String key, T value) {
        return redisRepository.set(key, value, defaultTtl);
    }

    public <T> Mono<Boolean> cacheData(String key, T value, Duration ttl) {
        return redisRepository.set(key, value, ttl);
    }

    public Mono<Boolean> invalidateCache(String key) {
        return redisRepository.delete(key);
    }
    
    public String generateCacheKey(String prefix, String identifier) {
        return redisRepository.generateKey(prefix, identifier);
    }
}
