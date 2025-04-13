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
        log.info("Kullanıcı tercihleri getiriliyor: userId={}", userId);
        return redisCacheService.getUserPreferences(userId)
            .doOnSuccess(pref -> log.debug("Kullanıcı tercihleri başarıyla getirildi: userId={}", userId))
            .doOnError(e -> log.error("Kullanıcı tercihleri getirilirken hata: userId={}, error={}", userId, e.getMessage()));
    }

    public Mono<UserPreference> getDefaultPreferences(String userId, Throwable t) {
        log.warn("Varsayılan tercihler döndürülüyor (fallback): userId={}, error={}", userId, t.getMessage());
        return Mono.just(UserPreference.builder()
                .userId(userId)
                .theme("light")
                .language("tr")
                .notifications(true)
                .pushEnabled(true)
                .createdAt(System.currentTimeMillis())
                .updatedAt(System.currentTimeMillis())
                .build());
    }

    public Mono<UserPreference> saveUserPreferences(UserPreference preferences) {
        log.info("Kullanıcı tercihleri kaydediliyor: userId={}", preferences.getUserId());
        return redisCacheService.saveUserPreferences(preferences)
                .then(Mono.defer(() -> {
                    // Kafka'ya bildirim gönder
                    kafkaTemplate.send(userPreferencesTopic, preferences.getUserId(), preferences);
                    return Mono.just(preferences);
                }))
                .doOnSuccess(pref -> log.debug("Kullanıcı tercihleri başarıyla kaydedildi: userId={}", preferences.getUserId()))
                .doOnError(e -> log.error("Kullanıcı tercihleri kaydedilirken hata: userId={}, error={}", 
                        preferences.getUserId(), e.getMessage()));
    }
    
    public Mono<UserPreference> updateTheme(String userId, String theme) {
        log.info("Tema güncelleniyor: userId={}, theme={}", userId, theme);
        return getUserPreferences(userId)
            .flatMap(pref -> {
                if (theme.equals(pref.getTheme())) {
                    log.info("Tema değeri zaten '{}' olarak ayarlı, güncelleme yapılmadı", theme);
                    return Mono.just(pref);
                }
                
                pref.setTheme(theme);
                pref.setUpdatedAt(System.currentTimeMillis());
                return redisCacheService.saveUserPreferences(pref)
                    .timeout(Duration.ofSeconds(8))
                    .thenReturn(pref);
            })
            .doOnNext(saved -> {
                log.debug("Tema başarıyla güncellendi: userId={}, theme={}", userId, theme);
                // Sadece tema değişikliğini event olarak yayınla
                publishPreferenceUpdated(userId, "theme", theme);
            })
            .doOnError(e -> log.error("Tema güncellenirken hata: userId={}, theme={}, error={}", 
                    userId, theme, e.getMessage()))
            .onErrorResume(e -> {
                log.warn("Tema güncelleme hatası için fallback çalıştırılıyor: userId={}", userId);
                return createFallbackThemePreference(userId, theme);
            });
    }

    private Mono<UserPreference> createFallbackThemePreference(String userId, String theme) {
        UserPreference fallbackPreference = UserPreference.builder()
                .userId(userId)
                .theme(theme)
                .language("tr")
                .notifications(true)
                .pushEnabled(true)
                .aiModelFavorites(new ArrayList<>())
                .createdAt(System.currentTimeMillis())
                .updatedAt(System.currentTimeMillis())
                .build();
        
        log.info("Fallback tema tercihi oluşturuldu: userId={}, theme={}", userId, theme);
        return Mono.just(fallbackPreference);
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
                    .timeout(Duration.ofSeconds(8)) // Timeout ekle
                    .thenReturn(pref);
            })
            .doOnNext(saved -> {
                log.debug("Dil başarıyla güncellendi: userId={}, language={}", userId, language);
                // Sadece dil değişikliğini event olarak yayınla
                publishPreferenceUpdated(userId, "language", language);
            })
            .doOnError(e -> log.error("Dil güncellenirken hata oluştu: userId={}, language={}, error={}", 
                    userId, language, e.getMessage()));
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
        log.info("Favori modeller güncelleniyor: userId={}, favoriteCount={}", userId, favorites.size());
        return getUserPreferences(userId)
            .flatMap(pref -> {
                pref.setAiModelFavorites(favorites);
                pref.setUpdatedAt(System.currentTimeMillis());
                return redisCacheService.saveUserPreferences(pref)
                    .timeout(Duration.ofSeconds(8))
                    .thenReturn(pref);
            })
            .doOnNext(saved -> {
                log.debug("Favori modeller başarıyla güncellendi: userId={}", userId);
                // Favori model değişikliğini event olarak yayınla
                publishPreferenceUpdated(userId, "aiModelFavorites", String.join(",", favorites));
            })
            .doOnError(e -> log.error("Favori modeller güncellenirken hata: userId={}, error={}", 
                    userId, e.getMessage()))
            .onErrorResume(e -> {
                log.warn("Favori modeller güncelleme hatası için fallback çalıştırılıyor: userId={}", userId);
                return createFallbackFavoritesPreference(userId, favorites);
            });
    }

    private Mono<UserPreference> createFallbackFavoritesPreference(String userId, List<String> favorites) {
        UserPreference fallbackPreference = UserPreference.builder()
                .userId(userId)
                .theme("light")
                .language("tr")
                .notifications(true)
                .pushEnabled(true)
                .aiModelFavorites(favorites)
                .createdAt(System.currentTimeMillis())
                .updatedAt(System.currentTimeMillis())
                .build();
        
        log.info("Fallback favori modeller tercihi oluşturuldu: userId={}", userId);
        return Mono.just(fallbackPreference);
    }

    public Mono<Boolean> deleteUserPreferences(String userId) {
        log.info("Kullanıcı tercihleri siliniyor: userId={}", userId);
        return redisCacheService.deleteUserPreferences(userId)
            .timeout(Duration.ofSeconds(8))
            .doOnSuccess(result -> {
                if (Boolean.TRUE.equals(result)) {
                    log.info("Kullanıcı tercihleri başarıyla silindi: userId={}", userId);
                    publishPreferenceUpdated(userId, "deleted", "true");
                } else {
                    log.warn("Kullanıcı tercihleri silinemedi (mevcut değil): userId={}", userId);
                }
            })
            .doOnError(e -> log.error("Kullanıcı tercihleri silinirken hata: userId={}, error={}", 
                    userId, e.getMessage()))
            .onErrorReturn(false);
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
}