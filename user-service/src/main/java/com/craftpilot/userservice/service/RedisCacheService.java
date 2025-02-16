package com.craftpilot.userservice.service;

import com.craftpilot.userservice.model.UserPreference;
import com.craftpilot.userservice.model.user.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RedisCacheService {
    private final ReactiveRedisTemplate<String, UserEntity> userRedisTemplate;
    private final ReactiveRedisTemplate<String, UserPreference> preferenceRedisTemplate;
    
    private static final String USER_KEY_PREFIX = "user:";
    private static final String PREFERENCE_KEY_PREFIX = "preference:";
    private static final Duration CACHE_TTL = Duration.ofHours(24);

    // Generic methods for UserEntity
    public Mono<UserEntity> get(String key) {
        return userRedisTemplate.opsForValue().get(USER_KEY_PREFIX + key);
    }

    public Mono<Boolean> set(String key, UserEntity value) {
        return userRedisTemplate.opsForValue()
                .set(USER_KEY_PREFIX + key, value, CACHE_TTL);
    }

    public Mono<Boolean> delete(String key) {
        return userRedisTemplate.opsForValue().delete(USER_KEY_PREFIX + key);
    }

    // Preference specific methods
    public Mono<UserPreference> getUserPreferences(String userId) {
        return preferenceRedisTemplate.opsForValue().get(PREFERENCE_KEY_PREFIX + userId);
    }

    public Mono<Boolean> saveUserPreferences(UserPreference preference) {
        return preferenceRedisTemplate.opsForValue()
                .set(PREFERENCE_KEY_PREFIX + preference.getUserId(), preference, CACHE_TTL);
    }

    public Mono<Boolean> deleteUserPreferences(String userId) {
        return preferenceRedisTemplate.opsForValue().delete(PREFERENCE_KEY_PREFIX + userId);
    }
}