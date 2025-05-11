package com.craftpilot.userservice.service;

import com.craftpilot.redis.service.ReactiveCacheService;
import com.craftpilot.userservice.model.UserPreference;
import com.craftpilot.userservice.model.user.entity.UserEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;

/**
 * Redis önbellekleme işlemlerini yöneten servis.
 * Bu servis, Redis Client Library'yi kullanarak Redis operasyonlarını gerçekleştirir.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RedisCacheService {
    
    private final ReactiveCacheService cacheService;
    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    
    private static final String USER_KEY_PREFIX = "user:";
    private static final String PREFERENCE_KEY_PREFIX = "preference:";
    private static final Duration DEFAULT_TTL = Duration.ofHours(1);

    // UserEntity için önbellekleme işlemleri
    public Mono<UserEntity> getUser(String userId) {
        log.debug("Kullanıcı Redis'den getiriliyor: userId={}", userId);
        return cacheService.getFromCache(USER_KEY_PREFIX + userId, UserEntity.class)
                .flatMap(optionalUser -> optionalUser.map(Mono::just).orElse(Mono.empty()));
    }

    public Mono<Boolean> saveUser(String userId, UserEntity user) {
        log.debug("Kullanıcı Redis'e kaydediliyor: userId={}", userId);
        return cacheService.cache(USER_KEY_PREFIX + userId, user);
    }

    public Mono<Boolean> deleteUser(String userId) {
        log.debug("Kullanıcı Redis'den siliniyor: userId={}", userId);
        return cacheService.delete(USER_KEY_PREFIX + userId);
    }

    // UserPreference için önbellekleme işlemleri
    public Mono<UserPreference> getUserPreferences(String userId) {
        log.debug("Kullanıcı tercihleri getiriliyor: userId={}", userId);
        return cacheService.getFromCache(PREFERENCE_KEY_PREFIX + userId, UserPreference.class)
                .flatMap(optionalPref -> {
                    if (optionalPref.isPresent()) {
                        return Mono.just(optionalPref.get());
                    } else {
                        // Varsayılan tercihleri oluştur
                        UserPreference defaultPref = UserPreference.builder()
                                .userId(userId)
                                .theme("light")
                                .language("tr")
                                .layout("collapsibleSide")
                                // Map<String, Boolean> oluştur
                                .notifications(new HashMap<String, Boolean>() {{
                                    put("general", true);
                                }})
                                .pushEnabled(true)
                                .createdAt(System.currentTimeMillis())
                                .updatedAt(System.currentTimeMillis())
                                .build();
                        log.debug("Varsayılan tercihler döndürülüyor: userId={}", userId);
                        return Mono.just(defaultPref);
                    }
                });
    }

    public Mono<Boolean> saveUserPreferences(UserPreference preference) {
        log.debug("Kullanıcı tercihleri Redis'e kaydediliyor: userId={}", preference.getUserId());
        preference.setUpdatedAt(System.currentTimeMillis());
        return cacheService.cache(PREFERENCE_KEY_PREFIX + preference.getUserId(), preference)
            .doOnSuccess(result -> {
                if (Boolean.TRUE.equals(result)) {
                    log.debug("Kullanıcı tercihleri başarıyla kaydedildi: userId={}", preference.getUserId());
                } else {
                    log.warn("Kullanıcı tercihleri kaydedilemedi: userId={}", preference.getUserId());
                }
            })
            .doOnError(err -> log.error("Kullanıcı tercihleri kaydedilirken hata: userId={}, error={}", 
                preference.getUserId(), err.getMessage()));
    }

    public Mono<Boolean> deleteUserPreferences(String userId) {
        log.debug("Kullanıcı tercihleri Redis'den siliniyor: userId={}", userId);
        return cacheService.delete(PREFERENCE_KEY_PREFIX + userId);
    }

    // Genel önbellekleme işlemleri
    public <T> Mono<T> getOrCache(String key, Class<T> type, Mono<T> fallback) {
        return cacheService.getOrCache(key, () -> fallback, type);
    }

    public <T> Mono<T> getOrCache(String key, Class<T> type, Mono<T> fallback, Duration ttl) {
        return cacheService.getOrCache(key, () -> fallback, type, ttl);
    }
    
    // Direct get and set methods used by UserService
    public Mono<String> get(String key) {
        log.debug("Redis'ten doğrudan değer alınıyor: key={}", key);
        return cacheService.get(key);
    }
    
    // Specific method for UserEntity to ensure type compatibility
    public Mono<Boolean> set(String key, UserEntity value) {
        log.debug("Redis'e doğrudan UserEntity kaydediliyor: key={}", key);
        return cacheService.cache(key, value);
    }
    
    // Generic set method for other types
    public <T> Mono<Boolean> setGeneric(String key, T value) {
        log.debug("Redis'e doğrudan değer kaydediliyor: key={}", key);
        return cacheService.cache(key, value);
    }

    public <T> Mono<String> set(String key, T value) {
        return set(key, value, DEFAULT_TTL);
    }
    
    public <T> Mono<String> set(String key, T value, Duration ttl) {
        try {
            String json = objectMapper.writeValueAsString(value);
            String cacheKey = USER_KEY_PREFIX + key;
            
            return redisTemplate.opsForValue().set(cacheKey, json, ttl)
                    .flatMap(success -> {
                        if (Boolean.TRUE.equals(success)) {
                            log.debug("Cache hit: {}", cacheKey);
                            return Mono.just(json);
                        } else {
                            log.warn("Failed to set cache key: {}", cacheKey);
                            return Mono.empty();
                        }
                    })
                    .onErrorResume(e -> {
                        log.error("Error setting cache: {}", e.getMessage());
                        return Mono.empty();
                    });
        } catch (JsonProcessingException e) {
            log.error("Error serializing object to JSON: {}", e.getMessage());
            return Mono.empty();
        }
    }
    
    // Duplicate get method removed

    public Mono<Boolean> delete(String key) {
        String cacheKey = USER_KEY_PREFIX + key;
        
        return redisTemplate.delete(cacheKey)
                .map(count -> count > 0)
                .doOnNext(deleted -> {
                    if (deleted) {
                        log.debug("Deleted cache key: {}", cacheKey);
                    } else {
                        log.debug("Cache key not found for deletion: {}", cacheKey);
                    }
                })
                .onErrorResume(e -> {
                    log.error("Error deleting from cache: {}", e.getMessage());
                    return Mono.just(false);
                });
    }
}