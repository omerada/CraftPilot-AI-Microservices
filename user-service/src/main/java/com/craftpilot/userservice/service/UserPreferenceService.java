package com.craftpilot.userservice.service;

import com.craftpilot.userservice.dto.UserPreferenceRequest;
import com.craftpilot.userservice.mapper.UserPreferenceMapper;
import com.craftpilot.userservice.model.UserPreference;
import com.craftpilot.userservice.model.PreferenceChangeEvent;
import com.craftpilot.userservice.repository.UserPreferenceRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.kafka.support.SendResult;
import io.micrometer.core.instrument.MeterRegistry;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserPreferenceService {
    private final UserPreferenceRepository userPreferenceRepository;
    private final RedisCacheService redisCacheService;
    private final EventService eventService;
    private final UserPreferenceMapper mapper;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final KafkaTemplate<String, String> kafkaTemplateString;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;
    
    @Value("${kafka.topics.user-preferences:user-preferences}")
    private String userPreferencesTopic;

    @Value("${user-preference.operation.timeout:1000}")
    private long operationTimeoutMillis;

    @Value("${kafka.topics.preference-events:preference-events}")
    private String preferencesEventsTopic;

    @CircuitBreaker(name = "user_preferences", fallbackMethod = "getDefaultPreferences")
    public Mono<UserPreference> getUserPreferences(String userId) {
        log.info("Kullanıcı tercihleri getiriliyor: userId={}", userId);
        return redisCacheService.getUserPreferences(userId)
                .switchIfEmpty(
                    userPreferenceRepository.findById(userId)
                        .flatMap(preferences -> {
                            // Redis önbelleğine kaydet
                            return redisCacheService.saveUserPreferences(preferences)
                                .thenReturn(preferences);
                        })
                        .switchIfEmpty(Mono.defer(() -> {
                            // Kullanıcı bulunamazsa varsayılan tercih oluştur
                            UserPreference defaultPrefs = createDefaultPreferences(userId);
                            return redisCacheService.saveUserPreferences(defaultPrefs)
                                .thenReturn(defaultPrefs);
                        }))
                )
                .doOnError(e -> log.error("Kullanıcı tercihleri getirilirken hata: userId={}, error={}", 
                        userId, e.getMessage()));
    }

    public Mono<UserPreference> getDefaultPreferences(String userId, Throwable t) {
        log.warn("Varsayılan tercihler döndürülüyor (fallback): userId={}, error={}", userId, t.getMessage());
        Map<String, Boolean> notificationsMap = new HashMap<>();
        notificationsMap.put("general", true); // veya false
        return Mono.just(UserPreference.builder()
                .userId(userId)
                .theme("light")
                .language("tr")
                .notifications(notificationsMap)
                .pushEnabled(true)
                .createdAt(System.currentTimeMillis())
                .updatedAt(System.currentTimeMillis())
                .build());
    }

    public Mono<UserPreference> saveUserPreferences(UserPreference userPreference) {
        log.info("Kullanıcı tercihleri kaydediliyor: userId={}", userPreference.getUserId());
        return userPreferenceRepository.save(userPreference)
            .flatMap(savedPreference -> {
                // savedPreference tipini UserPreference olarak belirtiyoruz
                UserPreference preference = (UserPreference) savedPreference;
                // metot adını düzeltiyoruz: saveUserPreference -> saveUserPreferences
                return redisCacheService.saveUserPreferences(preference)
                    .thenReturn(preference);
            })
            .doOnSuccess(savedPreference -> {
                // Event publishing'i non-blocking ve hata toleranslı hale getir
                eventService.publishPreferenceChangedEvent(savedPreference)
                    .subscribeOn(Schedulers.boundedElastic())
                    .subscribe(
                        null,
                        error -> log.error("Tercih değişikliği olayı yayınlanırken hata (event publish işlemi ana işlem akışını etkilemedi): userId={}, error={}", 
                                savedPreference.getUserId(), error.getMessage())
                    );
                log.info("Kullanıcı tercihleri başarıyla kaydedildi: userId={}", savedPreference.getUserId());
            })
            .doOnError(e -> log.error("Kullanıcı tercihleri kaydedilirken hata: userId={}, error={}", 
                    userPreference.getUserId(), e.getMessage()));
    }
    
    public Mono<UserPreference> updateTheme(String userId, String theme) {
        log.info("Tema güncelleniyor: userId={}, theme={}", userId, theme);
        return getUserPreferences(userId)
            .flatMap(existingPreference -> {
                existingPreference.setTheme(theme);
                existingPreference.setUpdatedAt(System.currentTimeMillis());
                return saveUserPreferences(existingPreference);
            })
            .doOnSuccess(updatedPreference -> 
                log.info("Tema başarıyla güncellendi: userId={}, theme={}", userId, theme));
    }

    private Mono<UserPreference> createFallbackThemePreference(String userId, String theme) {
        Map<String, Boolean> notificationsMap = new HashMap<>();
        notificationsMap.put("general", true); // veya false
        UserPreference fallbackPreference = UserPreference.builder()
                .userId(userId)
                .theme(theme)
                .language("tr")
                .notifications(notificationsMap)
                .pushEnabled(true)
                .aiModelFavorites(new ArrayList<>())
                .createdAt(System.currentTimeMillis())
                .updatedAt(System.currentTimeMillis())
                .build();
        
        log.info("Fallback tema tercihi oluşturuldu: userId={}, theme={}", userId, theme);
        return Mono.just(fallbackPreference);
    }

    @CircuitBreaker(name = "user_preferences", fallbackMethod = "updateLanguageFallback")
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
        
        Map<String, Boolean> notificationsMap = new HashMap<>();
        notificationsMap.put("general", true); // veya false
        // Basit bir UserPreference oluştur ve sadece dil değerini ayarla
        UserPreference fallbackPreference = UserPreference.builder()
                .userId(userId)
                .language(language)
                .theme("light") // varsayılan değer
                .notifications(notificationsMap) // varsayılan değer
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
        Map<String, Boolean> notificationsMap = new HashMap<>();
        notificationsMap.put("general", true); // veya false
        UserPreference fallbackPreference = UserPreference.builder()
                .userId(userId)
                .theme("light")
                .language("tr")
                .notifications(notificationsMap)
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

    public Mono<UserPreference> updateUserPreferences(String userId, Map<String, Object> updates) {
        log.info("Kullanıcı tercihleri güncelleniyor: userId={}", userId);
        return getUserPreferences(userId)
                .flatMap(existingPreferences -> {
                    // İlk kez oluşturuluyorsa createdAt ayarla
                    if (existingPreferences.getCreatedAt() == null) {
                        existingPreferences.setCreatedAt(System.currentTimeMillis());
                    }

                    // Gelen değerleri mevcut tercihlerle birleştir
                    mergePreferences(existingPreferences, updates);

                    // updatedAt'i güncelle
                    existingPreferences.setUpdatedAt(System.currentTimeMillis());

                    // Redis'e ve veritabanına kaydet
                    return redisCacheService.saveUserPreferences(existingPreferences)
                        .timeout(Duration.ofMillis(operationTimeoutMillis))
                        .doOnError(e -> {
                            if (e instanceof TimeoutException) {
                                log.warn("Redis kaydetme işlemi zaman aşımına uğradı, ancak işlem arka planda devam edecek: userId={}", userId);
                            } else {
                                log.error("Redis kaydetme işlemi sırasında hata: userId={}, error={}", userId, e.getMessage());
                            }
                        })
                        .onErrorResume(e -> Mono.empty())
                        .then(userPreferenceRepository.save(existingPreferences))
                        .doOnSuccess(saved -> {
                            // Kafka ile tercih değişikliği olayını yayınla
                            publishPreferenceUpdated(userId, updates);
                        });
                })
                .doOnError(e -> log.error("Kullanıcı tercihleri güncellenirken hata: userId={}, error={}", 
                        userId, e.getMessage()));
    }

    private void mergePreferences(UserPreference target, Map<String, Object> updates) {
        // theme güncelleme
        if (updates.containsKey("theme")) {
            target.setTheme((String) updates.get("theme"));
        }

        // themeSchema güncelleme
        if (updates.containsKey("themeSchema")) {
            target.setThemeSchema((String) updates.get("themeSchema"));
        }

        // language güncelleme
        if (updates.containsKey("language")) {
            target.setLanguage((String) updates.get("language"));
        }

        // layout güncelleme
        if (updates.containsKey("layout")) {
            target.setLayout((String) updates.get("layout"));
        }

        // aiModelFavorites güncelleme
        if (updates.containsKey("aiModelFavorites")) {
            target.setAiModelFavorites((List<String>) updates.get("aiModelFavorites"));
        }

        // lastSelectedModelId güncelleme
        if (updates.containsKey("lastSelectedModelId")) {
            target.setLastSelectedModelId((String) updates.get("lastSelectedModelId"));
        }

        // notifications güncelleme
        if (updates.containsKey("notifications")) {
            Map<String, Boolean> newNotifications = (Map<String, Boolean>) updates.get("notifications");
            if (target.getNotifications() == null) {
                target.setNotifications(new HashMap<>());
            }
            target.getNotifications().putAll(newNotifications);
        }

        // pushEnabled güncelleme
        if (updates.containsKey("pushEnabled")) {
            target.setPushEnabled((Boolean) updates.get("pushEnabled"));
        }
    }

    private UserPreference createDefaultPreferences(String userId) {
        log.info("Varsayılan kullanıcı tercihleri oluşturuluyor: userId={}", userId);
        
        Map<String, Boolean> notificationsMap = new HashMap<>();
        notificationsMap.put("general", true); // veya false
        UserPreference preferences = UserPreference.builder()
            .userId(userId)
            .theme("system")
            .themeSchema("default")
            .language("en")
            .layout("collapsibleSide")
            .aiModelFavorites(new ArrayList<>())
            .notifications(notificationsMap)
            .pushEnabled(false)
            .lastSelectedModelId("google/gemini-2.0-flash-lite-001") // Varsayılan model ID'si
            .build();

        // Zaman damgaları
        Long now = System.currentTimeMillis();
        preferences.setCreatedAt(now);
        preferences.setUpdatedAt(now);

        return preferences;
    }

    private void publishPreferenceUpdated(String userId, Map<String, Object> updates) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("userId", userId);
            event.put("eventType", "PREFERENCE_UPDATED");
            event.put("timestamp", System.currentTimeMillis());
            event.put("updates", updates);

            kafkaTemplate.send(preferencesEventsTopic, userId, event);
            log.debug("Tercih değişikliği olayı yayınlandı: userId={}", userId);
        } catch (Exception e) {
            log.error("Tercih değişikliği olayı yayınlanırken hata: userId={}, error={}", userId, e.getMessage());
        }
    }

    private void publishPreferenceUpdated(String userId, String preferenceType, String value) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("userId", userId);
            event.put("eventType", "PREFERENCES_UPDATED");
            event.put("preferenceType", preferenceType);
            event.put("value", value);
            event.put("timestamp", System.currentTimeMillis());
            
            // Asenkron gönderim - operasyonu bloklamıyor
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

    /**
     * Tercih değişikliği olayını Kafka'ya yayınlar, hata durumunda güvenli şekilde devam eder
     */
    private void publishPreferenceChangeEvent(String userId, UserPreference preference) {
        try {
            PreferenceChangeEvent event = new PreferenceChangeEvent();
            event.setUserId(userId);
            event.setTimestamp(System.currentTimeMillis());
            event.setPreferenceType("user_preference");
            event.setPreferenceData(objectMapper.convertValue(preference, new TypeReference<Map<String, Object>>() {}));
            
            String eventJson = objectMapper.writeValueAsString(event);
            
            CompletableFuture<SendResult<String, String>> future = kafkaTemplateString.send("user-preferences", userId, eventJson);
            
            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.debug("Tercih değişikliği olayı başarıyla yayınlandı: userId={}, topic={}, partition={}, offset={}", userId, result.getRecordMetadata().topic(), result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
                    
                    // Başarılı metrik
                    meterRegistry.counter("preference.kafka.publish.success", "userId", userId).increment();
                } else {
                    log.error("Tercih değişikliği olayı yayınlanırken hata: userId={}, error={}", userId, ex.getMessage());
                    // Hata metriği
                    meterRegistry.counter("preference.kafka.publish.error", "userId", userId).increment();
                }
            });
            
            // Deneme metriği
            meterRegistry.counter("preference.kafka.publish.attempt", "userId", userId).increment();
        } catch (Exception e) {
            log.error("Tercih değişikliği olayı yayınlanırken istisna: userId={}, error={}", userId, e.getMessage());
            // Hata durumunda işlemin devam etmesini sağla - kritik operasyon değil
            meterRegistry.counter("preference.kafka.error", "type", e.getClass().getSimpleName()).increment();
        }
    }
}