package com.craftpilot.userservice.service;

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
    private final UserPreferenceRepository preferenceRepository;
    private final RedisCacheService cacheService;

    public Mono<UserPreferenceResponse> getUserPreferences(String userId) {
        return cacheService.getUserPreferences(userId)
                .switchIfEmpty(Mono.defer(() -> 
                    preferenceRepository.findByUserId(userId)
                        .map(preferences -> UserPreferenceResponse.builder()
                            .userId(userId)
                            .theme(preferences.getTheme())
                            .language(preferences.getLanguage())
                            .notifications(preferences.isNotifications())
                            .build())
                        .doOnSuccess(preferences -> cacheService.cacheUserPreferences(userId, preferences))
                ));
    }

    public Mono<UserPreferenceResponse> updateUserPreferences(String userId, UserPreferenceResponse request) {
        return preferenceRepository.findByUserId(userId)
                .flatMap(existingPrefs -> {
                    existingPrefs.setTheme(request.getTheme());
                    existingPrefs.setLanguage(request.getLanguage());
                    existingPrefs.setNotifications(request.isNotifications());
                    return preferenceRepository.save(existingPrefs);
                })
                .map(updatedPrefs -> UserPreferenceResponse.builder()
                    .userId(userId)
                    .theme(updatedPrefs.getTheme())
                    .language(updatedPrefs.getLanguage())
                    .notifications(updatedPrefs.isNotifications())
                    .build())
                .doOnSuccess(preferences -> cacheService.cacheUserPreferences(userId, preferences));
    }
} 