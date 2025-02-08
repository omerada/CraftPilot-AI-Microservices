package com.craftpilot.userservice.service;

import com.craftpilot.userservice.model.UserPreference;
import com.craftpilot.userservice.model.UserPreferenceResponse;
import com.craftpilot.userservice.repository.UserPreferenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserPreferenceService {
    private final UserPreferenceRepository userPreferenceRepository;

    public Mono<UserPreferenceResponse> getUserPreferences(String userId) {
        return userPreferenceRepository.findById(userId)
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

        return userPreferenceRepository.save(updatedPrefs)
                .then(Mono.just(request));
    }

    public Mono<Boolean> getNotificationPreference(String userId) {
        return userPreferenceRepository.findById(userId)
                .map(UserPreference::isNotifications)
                .defaultIfEmpty(true); // Default to true if no preference is set
    }

    public Mono<UserPreference> updateNotificationPreference(String userId, boolean enabled) {
        return userPreferenceRepository.findById(userId)
                .defaultIfEmpty(UserPreference.builder().userId(userId).build())
                .map(preferences -> UserPreference.builder()
                        .userId(preferences.getUserId())
                        .notifications(enabled)
                        .theme(preferences.getTheme())
                        .language(preferences.getLanguage())
                        .build())
                .flatMap(userPreferenceRepository::save);
    }
}