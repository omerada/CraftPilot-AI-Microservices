package com.craftpilot.contentservice.repository;

import com.craftpilot.contentservice.model.Content;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Slf4j
@Repository
@RequiredArgsConstructor
public class RedisContentCacheRepository implements ContentCacheRepository {
    private static final String KEY_PREFIX = "content:";
    private static final Duration CACHE_TTL = Duration.ofHours(24);

    private final ReactiveRedisTemplate<String, Content> redisTemplate;

    @Override
    public Mono<Content> save(Content content) {
        String key = KEY_PREFIX + content.getId();
        return redisTemplate.opsForValue()
                .set(key, content, CACHE_TTL)
                .thenReturn(content)
                .doOnSuccess(saved -> log.debug("Content cached successfully: {}", key))
                .doOnError(error -> log.error("Error caching content: {}", error.getMessage()));
    }

    @Override
    public Mono<Content> findById(String id) {
        String key = KEY_PREFIX + id;
        return redisTemplate.opsForValue()
                .get(key)
                .doOnSuccess(content -> {
                    if (content != null) {
                        log.debug("Content found in cache: {}", key);
                    } else {
                        log.debug("Content not found in cache: {}", key);
                    }
                })
                .doOnError(error -> log.error("Error retrieving content from cache: {}", error.getMessage()));
    }

    @Override
    public Mono<Void> deleteById(String id) {
        String key = KEY_PREFIX + id;
        return redisTemplate.delete(key)
                .then()
                .doOnSuccess(v -> log.debug("Content removed from cache: {}", key))
                .doOnError(error -> log.error("Error removing content from cache: {}", error.getMessage()));
    }

    @Override
    public Mono<Void> deleteAll() {
        return redisTemplate.keys(KEY_PREFIX + "*")
                .flatMap(redisTemplate::delete)
                .then()
                .doOnSuccess(v -> log.debug("All content removed from cache"))
                .doOnError(error -> log.error("Error removing all content from cache: {}", error.getMessage()));
    }
} 