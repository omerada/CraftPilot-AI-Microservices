package com.craftpilot.userservice.service;

import com.craftpilot.userservice.model.user.entity.UserEntity;
import com.craftpilot.userservice.model.UserPreference;
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
    private final ReactiveRedisTemplate<String, UserPreference> userPreferenceRedisTemplate;
    private static final Duration CACHE_TTL = Duration.ofHours(1);
    private static final String USER_KEY_PREFIX = "user:";
    private static final String PREFERENCE_KEY_PREFIX = "preference:";

    public Mono<UserEntity> getCachedUser(String userId) {
        return redisTemplate.opsForValue().get(getUserKey(userId));
    }

    public Mono<Void> cacheUser(UserEntity user) {
        return redisTemplate.opsForValue()
                .set(getUserKey(user.getId()), user, CACHE_TTL)
                .then();
    }

    public Mono<UserPreference> getUserPreferences(String userId) {
        return userPreferenceRedisTemplate.opsForValue()
                .get(getPreferenceKey(userId));
    }

    public Mono<UserPreference> saveUserPreferences(UserPreference preferences) {
        return userPreferenceRedisTemplate.opsForValue()
                .set(getPreferenceKey(preferences.getUserId()), preferences, CACHE_TTL)
                .thenReturn(preferences);
    }

    public Mono<Void> deleteUserPreferences(String userId) {
        return userPreferenceRedisTemplate.opsForValue()
                .delete(getPreferenceKey(userId))
                .then();
    }

    private String getUserKey(String userId) {
        return USER_KEY_PREFIX + userId;
    }

    private String getPreferenceKey(String userId) {
        return PREFERENCE_KEY_PREFIX + userId;
    }
}