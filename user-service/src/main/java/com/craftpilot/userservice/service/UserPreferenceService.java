package com.craftpilot.userservice.service;

import com.craftpilot.userservice.model.UserPreference;
import com.craftpilot.userservice.repository.UserPreferenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class UserPreferenceService {
    private final RedisCacheService redisCacheService;

    public Mono<UserPreference> getDefaultPreferences(String userId) {
        return Mono.just(UserPreference.builder()
                .userId(userId)
                .theme("light")
                .language("en")
                .notifications(true)
                .pushEnabled(true)
                .build());
    }

    public Mono<UserPreference> getUserPreferences(String userId) {
        return redisCacheService.getUserPreferences(userId)
                .switchIfEmpty(getDefaultPreferences(userId));
    }

    public Mono<UserPreference> updateUserPreferences(String userId, UserPreference preferences) {
        preferences.setUserId(userId);
        return redisCacheService.saveUserPreferences(preferences);
    }
}