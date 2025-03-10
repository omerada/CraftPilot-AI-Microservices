package com.craftpilot.userservice.service;

import com.craftpilot.userservice.dto.UserPreferenceRequest;
import com.craftpilot.userservice.mapper.UserPreferenceMapper;
import com.craftpilot.userservice.model.UserPreference;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserPreferenceService {
    private final RedisCacheService redisCacheService;
    private final UserPreferenceMapper mapper;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    @Value("${kafka.topics.user-preferences}")
    private String userPreferencesTopic;

    @CircuitBreaker(name = "userPreferences", fallbackMethod = "getDefaultPreferences")
    public Mono<UserPreference> getUserPreferences(String userId) {
        return redisCacheService.getUserPreferences(userId);
    }

    public Mono<UserPreference> saveUserPreferences(UserPreference preferences) {
        return redisCacheService.saveUserPreferences(preferences)
                .thenReturn(preferences)
                .doOnNext(saved -> {
                    // Preference değişikliğini event olarak yayınla
                    publishPreferenceUpdated(saved.getUserId(), "language", saved.getLanguage());
                });
    }
    
    public Mono<UserPreference> updateLanguage(String userId, String language) {
        return getUserPreferences(userId)
            .flatMap(pref -> {
                pref.setLanguage(language);
                return redisCacheService.saveUserPreferences(pref)
                    .thenReturn(pref);
            })
            .doOnNext(saved -> {
                // Sadece dil değişikliğini event olarak yayınla
                publishPreferenceUpdated(userId, "language", language);
            });
    }

    public Mono<Boolean> deleteUserPreferences(String userId) {
        return redisCacheService.deleteUserPreferences(userId);
    }

    private void publishPreferenceUpdated(String userId, String preferenceType, String value) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("userId", userId);
            event.put("eventType", "PREFERENCES_UPDATED");
            event.put("preferenceType", preferenceType);
            event.put("value", value);
            event.put("timestamp", System.currentTimeMillis());
            
            kafkaTemplate.send(userPreferencesTopic, userId, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish preference update event: {}", ex.getMessage());
                    } else {
                        log.debug("Preference update event published for user: {}", userId);
                    }
                });
        } catch (Exception e) {
            log.error("Error publishing preference update: {}", e.getMessage());
        }
    }

    private Mono<UserPreference> getDefaultPreferences(String userId, Throwable t) {
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