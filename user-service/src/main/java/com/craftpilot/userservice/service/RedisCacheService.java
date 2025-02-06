package com.craftpilot.userservice.service;

import com.craftpilot.userservice.model.UserPreference;
import com.craftpilot.userservice.model.user.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisCacheService {
    private final ReactiveRedisTemplate<String, UserEntity> userRedisTemplate;
    private final ReactiveRedisTemplate<String, UserPreference> preferenceRedisTemplate;
    
    private static final String USER_KEY = "user:";
    private static final String USER_PREFERENCES_KEY = "user:preferences:";
    private static final long CACHE_DURATION = 3600; // 1 saat

    public Mono<UserEntity> cacheUser(UserEntity user) {
        return userRedisTemplate.opsForValue()
                .set(USER_KEY + user.getId(), user)
                .thenReturn(user)
                .doOnSuccess(u -> log.debug("User cached: {}", u.getId()))
                .doOnError(e -> log.error("Error caching user: {}", e.getMessage()));
    }

    public Mono<UserEntity> getCachedUser(String userId) {
        return userRedisTemplate.opsForValue()
                .get(USER_KEY + userId);
    }

    public Mono<UserPreference> getUserPreferences(String userId) {
        return preferenceRedisTemplate.opsForValue()
                .get(USER_PREFERENCES_KEY + userId)
                .doOnSuccess(prefs -> log.debug("Retrieved preferences for user: {}", userId))
                .doOnError(e -> log.error("Error getting preferences: {}", e.getMessage()));
    }

    public Mono<Void> cacheUserPreferences(String userId, UserPreference preferences) {
        return preferenceRedisTemplate.opsForValue()
                .set(USER_PREFERENCES_KEY + userId, preferences)
                .then();
    }
} 