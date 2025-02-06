package com.craftpilot.userservice.service;

import com.craftpilot.userservice.model.UserPreference;
import com.craftpilot.userservice.model.UserPreferenceResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserPreferenceService {
    private final RedisCacheService redisCacheService;

    public Mono<UserPreferenceResponse> getUserPreferences(String userId) {
        return redisCacheService.getUserPreferences(userId)
                .map(preferences -> UserPreferenceResponse.builder()
                        .theme(preferences.getTheme())
                        .language(preferences.getLanguage())
                        .notifications(preferences.isNotifications())
                        .build());
    }

    public Mono<UserPreferenceResponse> updateUserPreferences(String userId, UserPreferenceResponse request) {
        UserPreference updatedPrefs = UserPreference.builder()
                .userId(userId)
                .theme(request.getTheme())
                .language(request.getLanguage())
                .notifications(request.isNotifications())
                .build();

        return redisCacheService.cacheUserPreferences(userId, updatedPrefs)
                .then(Mono.just(request));
    }
} 