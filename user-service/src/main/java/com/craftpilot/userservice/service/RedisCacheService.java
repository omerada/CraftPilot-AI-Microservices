package com.craftpilot.userservice.service;

import com.craftpilot.userservice.model.UserPreferenceResponse;
import com.craftpilot.userservice.model.user.entity.UserEntity;
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

    private final ReactiveRedisTemplate<String, UserEntity> userRedisTemplate;
    private final ReactiveRedisTemplate<String, UserPreferenceResponse> preferenceRedisTemplate;
    private static final Duration CACHE_TTL = Duration.ofHours(1);
    private static final String USER_KEY_PREFIX = "user:";
    private static final String USER_PREFERENCES_KEY = "user:preferences:";

    public Mono<UserEntity> cacheUser(UserEntity user) {
        String key = USER_KEY_PREFIX + user.getId();
        return userRedisTemplate.opsForValue()
                .set(key, user, CACHE_TTL)
                .thenReturn(user);
    }

    public Mono<UserEntity> getCachedUser(String userId) {
        String key = USER_KEY_PREFIX + userId;
        return userRedisTemplate.opsForValue().get(key)
                .doOnNext(user -> log.debug("Cache hit for user: {}", userId))
                .doOnError(error -> log.error("Error getting user from cache: {}", error.getMessage()));
    }

    public Mono<Boolean> invalidateUser(String userId) {
        return userRedisTemplate.opsForValue()
                .delete(USER_KEY_PREFIX + userId);
    }

    public Mono<Boolean> hasUser(String userId) {
        String key = USER_KEY_PREFIX + userId;
        return userRedisTemplate.hasKey(key)
                .doOnSuccess(exists -> log.debug("Cache check for user {}: {}", key, exists))
                .doOnError(e -> log.error("Error checking user cache: {}", key, e));
    }

    public Mono<UserPreferenceResponse> getUserPreferences(String userId) {
        return preferenceRedisTemplate.opsForValue().get(USER_PREFERENCES_KEY + userId);
    }

    public Mono<Void> cacheUserPreferences(String userId, UserPreferenceResponse preferences) {
        return preferenceRedisTemplate.opsForValue()
                .set(USER_PREFERENCES_KEY + userId, preferences)
                .then();
    }
} 