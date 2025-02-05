package com.craftpilot.userservice.service;

import com.craftpilot.userservice.model.user.entity.UserEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisCacheService {

    private final ReactiveRedisTemplate<String, UserEntity> redisTemplate;
    private final ObjectMapper objectMapper;
    private static final Duration CACHE_TTL = Duration.ofHours(1);
    private static final String USER_KEY_PREFIX = "user:";

    public Mono<UserEntity> cacheUser(UserEntity user) {
        String key = USER_KEY_PREFIX + user.getId();
        return redisTemplate.opsForValue()
                .set(key, user, CACHE_TTL)
                .thenReturn(user);
    }

    public Mono<UserEntity> getCachedUser(String userId) {
        String key = USER_KEY_PREFIX + userId;
        return redisTemplate.opsForValue().get(key)
                .doOnNext(user -> log.debug("Cache hit for user: {}", userId))
                .doOnError(error -> log.error("Error getting user from cache: {}", error.getMessage()));
    }

    public Mono<Boolean> invalidateUser(String userId) {
        return redisTemplate.opsForValue()
                .delete(USER_KEY_PREFIX + userId);
    }

    public Mono<Boolean> hasUser(String userId) {
        String key = USER_KEY_PREFIX + userId;
        return redisTemplate.hasKey(key)
                .doOnSuccess(exists -> log.debug("Cache check for user {}: {}", key, exists))
                .doOnError(e -> log.error("Error checking user cache: {}", key, e));
    }
} 