package com.craftpilot.userservice.service;

import com.craftpilot.userservice.dto.UserPreferenceRequest;
import com.craftpilot.userservice.mapper.UserPreferenceMapper;
import com.craftpilot.userservice.model.UserPreference;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserPreferenceService {
    private final RedisCacheService redisCacheService;
    private final UserPreferenceMapper mapper;

    @CircuitBreaker(name = "userPreferences", fallbackMethod = "getDefaultPreferences")
    public Mono<UserPreference> getUserPreferences(String userId) {
        return redisCacheService.getUserPreferences(userId)
                .switchIfEmpty(getDefaultPreferences(userId));
    }

    public Mono<UserPreference> saveUserPreferences(String userId, UserPreferenceRequest request) {
        UserPreference preference = mapper.toEntity(request);
        preference.setUserId(userId);
        preference.setCreatedAt(System.currentTimeMillis());
        preference.setUpdatedAt(System.currentTimeMillis());
        return redisCacheService.saveUserPreferences(preference);
    }

    public Mono<Void> deleteUserPreferences(String userId) {
        return redisCacheService.deleteUserPreferences(userId);
    }

    private Mono<UserPreference> getDefaultPreferences(String userId) {
        return Mono.just(UserPreference.builder()
                .userId(userId)
                .theme("light")
                .language("en")
                .notifications(true)
                .pushEnabled(true)
                .createdAt(System.currentTimeMillis())
                .updatedAt(System.currentTimeMillis())
                .build());
    }
}