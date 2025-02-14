package com.craftpilot.userservice.service;

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
    private final ReactiveRedisTemplate<String, UserPreference> preferenceRedisTemplate;
    private static final String USER_PREFERENCES_KEY = "user:preferences:";
    private static final Duration CACHE_DURATION = Duration.ofHours(1);

    public Mono<UserPreference> getUserPreferences(String userId) {
        return preferenceRedisTemplate.opsForValue()
                .get(USER_PREFERENCES_KEY + userId)
                .doOnSuccess(prefs -> log.debug("Retrieved preferences for user: {}", userId))
                .doOnError(e -> log.error("Error getting preferences: {}", e.getMessage()));
    }

    public Mono<UserPreference> saveUserPreferences(UserPreference userPreference) {
        String key = USER_PREFERENCES_KEY + userPreference.getUserId();
        return preferenceRedisTemplate.opsForValue()
            .set(key, userPreference, CACHE_DURATION)
            .thenReturn(userPreference)
            .doOnSuccess(prefs -> log.debug("Saved preferences for user: {}", userPreference.getUserId()))
            .doOnError(e -> log.error("Error saving preferences: {}", e.getMessage()));
    }

    public Mono<Void> deleteUserPreferences(String userId) {
        return preferenceRedisTemplate.opsForValue()
            .delete(USER_PREFERENCES_KEY + userId)
            .doOnSuccess(v -> log.debug("Deleted preferences for user: {}", userId))
            .doOnError(e -> log.error("Error deleting preferences: {}", e.getMessage()));
    }
}