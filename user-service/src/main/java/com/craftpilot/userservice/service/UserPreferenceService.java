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

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserPreferenceService {
    private final RedisCacheService redisCacheService;
    private final UserPreferenceMapper mapper;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    @Value("${kafka.topics.user-preferences:user-preferences}")
    private String userPreferencesTopic;

    @CircuitBreaker(name = "userPreferences", fallbackMethod = "getDefaultPreferences")
    public Mono<UserPreference> getUserPreferences(String userId) {
        return redisCacheService.getUserPreferences(userId);
    }

    public Mono<UserPreference> saveUserPreferences(UserPreference preferences) {
        if (preferences.getCreatedAt() == null) {
            preferences.setCreatedAt(System.currentTimeMillis());
        }
        preferences.setUpdatedAt(System.currentTimeMillis());
        
        return redisCacheService.saveUserPreferences(preferences)
                .thenReturn(preferences)
                .doOnNext(saved -> {
                    // Tüm tercihleri event olarak yayınla
                    publishPreferenceUpdated(saved.getUserId(), "all", "");
                });
    }
    
    public Mono<UserPreference> updateTheme(String userId, String theme) {
        return getUserPreferences(userId)
            .flatMap(pref -> {
                pref.setTheme(theme);
                pref.setUpdatedAt(System.currentTimeMillis());
                return redisCacheService.saveUserPreferences(pref)
                    .thenReturn(pref);
            })
            .doOnNext(saved -> {
                // Sadece tema değişikliğini event olarak yayınla
                publishPreferenceUpdated(userId, "theme", theme);
            });
    }

    @CircuitBreaker(name = "userPreferences", fallbackMethod = "updateLanguageFallback")
    public Mono<UserPreference> updateLanguage(String userId, String language) {
        log.info("Dil güncelleniyor: userId={}, language={}", userId, language);
        return getUserPreferences(userId)
            .flatMap(pref -> {
                // Aynı değer ise işlem yapmaya gerek yok, mevcut tercihi döndür
                if (language.equals(pref.getLanguage())) {
                    log.info("Dil değeri zaten '{}' olarak ayarlı, güncelleme yapılmadı", language);
                    return Mono.just(pref);
                }
                
                pref.setLanguage(language);
                pref.setUpdatedAt(System.currentTimeMillis());
                return redisCacheService.saveUserPreferences(pref)
                    .timeout(Duration.ofSeconds(5)) // Timeout ekle
                    .thenReturn(pref);
            })
            .doOnNext(saved -> {
                // Sadece dil değişikliğini event olarak yayınla
                publishPreferenceUpdated(userId, "language", language);
            })
            .doOnError(e -> log.error("Dil güncellenirken hata oluştu: {}", e.getMessage()));
    }

    public Mono<UserPreference> updateLanguageFallback(String userId, String language, Throwable t) {
        log.warn("Dil güncelleme işlemi için fallback çalıştırıldı: userId={}, error={}", userId, t.getMessage());
        
        // Basit bir UserPreference oluştur ve sadece dil değerini ayarla
        UserPreference fallbackPreference = UserPreference.builder()
                .userId(userId)
                .language(language)
                .theme("light") // varsayılan değer
                .notifications(true) // varsayılan değer
                .pushEnabled(true) // varsayılan değer
                .aiModelFavorites(new ArrayList<>())
                .updatedAt(System.currentTimeMillis())
                .build();
        
        // Asenkron olarak tercihleri güncelleme işlemi başlat
        redisCacheService.saveUserPreferences(fallbackPreference)
            .subscribe(
                success -> log.info("Fallback sonrası asenkron güncelleme başarılı: userId={}", userId),
                error -> log.error("Fallback sonrası asenkron güncelleme başarısız: userId={}, error={}", userId, error.getMessage())
            );
        
        return Mono.just(fallbackPreference);
    }
    
    public Mono<UserPreference> updateAiModelFavorites(String userId, List<String> favorites) {
        return getUserPreferences(userId)
            .flatMap(pref -> {
                pref.setAiModelFavorites(favorites);
                pref.setUpdatedAt(System.currentTimeMillis());
                return redisCacheService.saveUserPreferences(pref)
                    .thenReturn(pref);
            })
            .doOnNext(saved -> {
                // Favori model değişikliğini event olarak yayınla
                publishPreferenceUpdated(userId, "aiModelFavorites", String.join(",", favorites));
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
        log.warn("Falling back to default preferences for user: {}, error: {}", userId, t.getMessage());
        return Mono.just(UserPreference.builder()
                .userId(userId)
                .theme("light")
                .language("tr")
                .notifications(true)
                .pushEnabled(true)
                .aiModelFavorites(new ArrayList<>())
                .createdAt(System.currentTimeMillis())
                .updatedAt(System.currentTimeMillis())
                .build());
    }
}