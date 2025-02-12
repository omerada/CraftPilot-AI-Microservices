package com.craftpilot.userservice.service;

import com.craftpilot.userservice.model.UserPreference;
import com.craftpilot.userservice.repository.UserPreferenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class UserPreferenceService {
    private final UserPreferenceRepository userPreferenceRepository;

    public Mono<UserPreference> getUserPreferences(String userId) {
        return userPreferenceRepository.findById(userId)
                .defaultIfEmpty(UserPreference.builder()
                        .userId(userId)
                        .theme("light")
                        .language("en")
                        .notifications(true)
                        .pushEnabled(true)
                        .build());
    }

    public Mono<UserPreference> updateUserPreferences(String userId, UserPreference preferences) {
        return userPreferenceRepository.findById(userId)
                .map(existing -> {
                    existing.setTheme(preferences.getTheme());
                    existing.setLanguage(preferences.getLanguage());
                    existing.setNotifications(preferences.isNotifications());
                    existing.setPushEnabled(preferences.isPushEnabled());
                    return existing;
                })
                .defaultIfEmpty(UserPreference.builder()
                        .userId(userId)
                        .theme(preferences.getTheme())
                        .language(preferences.getLanguage())
                        .notifications(preferences.isNotifications())
                        .pushEnabled(preferences.isPushEnabled())
                        .build())
                .flatMap(userPreferenceRepository::save);
    }

    public Mono<Void> deleteUserPreferences(String userId) {
        return userPreferenceRepository.deleteById(userId);
    }
}