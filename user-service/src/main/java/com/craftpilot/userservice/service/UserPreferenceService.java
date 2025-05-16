package com.craftpilot.userservice.service;

import com.craftpilot.userservice.dto.UserPreferenceRequest;
import com.craftpilot.userservice.event.UserPreferenceEvent;
import com.craftpilot.userservice.model.UserPreference;
import com.craftpilot.userservice.repository.UserPreferenceRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

@Service
@Slf4j
public class UserPreferenceService {

    private final UserPreferenceRepository userPreferenceRepository;
    private final RedisCacheService redisCacheService;
    private final EventService eventService;
    private final KafkaTemplate<String, UserPreferenceEvent> kafkaTemplate;

    @Value("${kafka.enabled:true}")
    private boolean kafkaEnabled;

    @Value("${spring.kafka.topic.user-preferences:user-preferences}")
    private String userPreferencesTopic;

    @Autowired
    public UserPreferenceService(
            UserPreferenceRepository userPreferenceRepository,
            RedisCacheService redisCacheService,
            EventService eventService,
            KafkaTemplate<String, UserPreferenceEvent> kafkaTemplate) {
        this.userPreferenceRepository = userPreferenceRepository;
        this.redisCacheService = redisCacheService;
        this.eventService = eventService;
        this.kafkaTemplate = kafkaTemplate;
        log.info("UserPreferenceService başlatıldı, Kafka durumu: {}", kafkaEnabled ? "etkin" : "devre dışı");
    }

    public Mono<UserPreference> getUserPreferences(String userId) {
        return redisCacheService.getUserPreferences(userId)
                .switchIfEmpty(userPreferenceRepository.findById(userId)
                        .flatMap(preferences -> redisCacheService.saveUserPreferences(preferences))
                        .switchIfEmpty(Mono.defer(() -> {
                            log.info("Kullanıcı tercihleri bulunamadı, varsayılan oluşturuluyor: userId={}", userId);
                            return createDefaultPreferences(userId);
                        })));
    }

    private Mono<UserPreference> createDefaultPreferences(String userId) {
        UserPreference defaultPreferences = UserPreference.createDefaultPreference(userId);
        return userPreferenceRepository.save(defaultPreferences)
                .flatMap(savedPreferences -> redisCacheService.saveUserPreferences(savedPreferences))
                .doOnSuccess(savedPreference -> {
                    log.info("Varsayılan kullanıcı tercihleri oluşturuldu: userId={}", savedPreference.getUserId());
                    sendUserPreferenceEvent(savedPreference, "USER_PREFERENCE_CREATED");
                });
    }

    public Mono<UserPreference> updateUserPreferences(String userId, Map<String, Object> updates) {
        return getUserPreferences(userId)
                .flatMap(existingPreferences -> {
                    try {
                        applyUpdates(existingPreferences, updates);
                        existingPreferences.setUpdatedAt(System.currentTimeMillis());
                        return userPreferenceRepository.save(existingPreferences)
                                .flatMap(savedPreference -> redisCacheService.saveUserPreferences(savedPreference))
                                .doOnSuccess(savedPreference -> {
                                    log.info("Kullanıcı tercihleri güncellendi: userId={}",
                                            savedPreference.getUserId());
                                    sendUserPreferenceEvent(savedPreference, "USER_PREFERENCE_UPDATED");
                                });
                    } catch (Exception e) {
                        log.error("Kullanıcı tercihleri güncellenirken hata: userId={}, error={}", userId,
                                e.getMessage());
                        return Mono.error(e);
                    }
                });
    }

    private void applyUpdates(UserPreference preferences, Map<String, Object> updates) {
        updates.forEach((key, value) -> {
            switch (key) {
                case "theme":
                    preferences.setTheme((String) value);
                    break;
                case "themeSchema":
                    preferences.setThemeSchema((String) value);
                    break;
                case "language":
                    preferences.setLanguage((String) value);
                    break;
                case "layout":
                    preferences.setLayout((String) value);
                    break;
                case "pushEnabled":
                    preferences.setPushEnabled((Boolean) value);
                    break;
                case "lastSelectedModelId":
                    preferences.setLastSelectedModelId((String) value);
                    break;
                case "aiModelFavorites":
                    if (value instanceof List) {
                        preferences.setAiModelFavorites((List<String>) value);
                    }
                    break;
                case "notifications":
                    if (value instanceof Map) {
                        preferences.setNotifications((Map<String, Boolean>) value);
                    }
                    break;
                default:
                    log.warn("Bilinmeyen tercih alanı: {}", key);
            }
        });
    }

    public Mono<UserPreference> updateTheme(String userId, String theme) {
        return getUserPreferences(userId)
                .flatMap(preferences -> {
                    preferences.setTheme(theme);
                    preferences.setUpdatedAt(System.currentTimeMillis());
                    return userPreferenceRepository.save(preferences)
                            .flatMap(savedPreference -> redisCacheService.saveUserPreferences(savedPreference))
                            .doOnSuccess(savedPreference -> {
                                log.info("Kullanıcı tema tercihi güncellendi: userId={}, theme={}",
                                        savedPreference.getUserId(), theme);
                                sendUserPreferenceEvent(savedPreference, "USER_THEME_UPDATED");
                            });
                });
    }

    public Mono<UserPreference> updateLanguage(String userId, String language) {
        return getUserPreferences(userId)
                .flatMap(preferences -> {
                    preferences.setLanguage(language);
                    preferences.setUpdatedAt(System.currentTimeMillis());
                    return userPreferenceRepository.save(preferences)
                            .flatMap(savedPreference -> redisCacheService.saveUserPreferences(savedPreference))
                            .doOnSuccess(savedPreference -> {
                                log.info("Kullanıcı dil tercihi güncellendi: userId={}, language={}",
                                        savedPreference.getUserId(), language);
                                sendUserPreferenceEvent(savedPreference, "USER_LANGUAGE_UPDATED");
                            });
                });
    }

    public Mono<UserPreference> updateAiModelFavorites(String userId, List<String> favorites) {
        return getUserPreferences(userId)
                .flatMap(preferences -> {
                    preferences.setAiModelFavorites(favorites);
                    preferences.setUpdatedAt(System.currentTimeMillis());
                    return userPreferenceRepository.save(preferences)
                            .flatMap(savedPreference -> redisCacheService.saveUserPreferences(savedPreference))
                            .doOnSuccess(savedPreference -> {
                                log.info("Kullanıcı favori AI modelleri güncellendi: userId={}",
                                        savedPreference.getUserId());
                                sendUserPreferenceEvent(savedPreference, "USER_FAVORITES_UPDATED");
                            });
                });
    }

    public Mono<Void> deleteUserPreferences(String userId) {
        return userPreferenceRepository.deleteById(userId)
                .then(redisCacheService.deleteUserPreferences(userId))
                .doOnSuccess(unused -> {
                    log.info("Kullanıcı tercihleri silindi: userId={}", userId);

                    // Kullanıcı silme olayını gönder
                    UserPreferenceEvent event = UserPreferenceEvent.builder()
                            .userId(userId)
                            .eventType("USER_PREFERENCE_DELETED")
                            .timestamp(System.currentTimeMillis())
                            .build();

                    if (kafkaEnabled) {
                        kafkaTemplate.send(userPreferencesTopic, userId, event)
                                .whenComplete((result, ex) -> {
                                    if (ex != null) {
                                        log.error("Tercih silme olayı yayınlanamadı: userId={}, error={}",
                                                userId, ex.getMessage());
                                    }
                                });
                    }
                });
    }

    public Mono<UserPreference> saveUserPreferences(UserPreference preferences) {
        preferences.setUpdatedAt(System.currentTimeMillis());
        return userPreferenceRepository.save(preferences)
                .flatMap(savedPreference -> redisCacheService.saveUserPreferences(savedPreference))
                .doOnSuccess(savedPreference -> {
                    log.info("Kullanıcı tercihleri kaydedildi: userId={}", savedPreference.getUserId());
                    sendUserPreferenceEvent(savedPreference, "USER_PREFERENCE_SAVED");
                });
    }

    private void sendUserPreferenceEvent(UserPreference preference, String eventType) {
        if (!kafkaEnabled || kafkaTemplate == null) {
            log.debug("Kafka devre dışı veya kullanılamıyor, olay gönderilmedi: userId={}, eventType={}",
                    preference.getUserId(), eventType);
            return;
        }

        UserPreferenceEvent event = UserPreferenceEvent.builder()
                .userId(preference.getUserId())
                .eventType(eventType)
                .timestamp(System.currentTimeMillis())
                .theme(preference.getTheme())
                .language(preference.getLanguage())
                .pushEnabled(preference.getPushEnabled())
                .build();

        try {
            kafkaTemplate.send(userPreferencesTopic, preference.getUserId(), event)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Tercih olayı yayınlanamadı: userId={}, eventType={}, error={}",
                                    preference.getUserId(), eventType, ex.getMessage());
                        } else {
                            log.debug("Tercih olayı yayınlandı: userId={}, eventType={}",
                                    preference.getUserId(), eventType);
                        }
                    });
        } catch (Exception e) {
            log.warn("Kafka olayı gönderilemedi: {}", e.getMessage());
        }
    }
}