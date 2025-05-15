package com.craftpilot.userservice.service;

import com.craftpilot.userservice.model.UserPreference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisCacheService {

    private final ReactiveRedisTemplate<String, Object> redisTemplate;
    private static final String USER_PREFERENCES_KEY_PREFIX = "user:preferences:";
    private static final Duration CACHE_TTL = Duration.ofHours(24);
    private static final Duration DEFAULT_TTL = CACHE_TTL;

    /**
     * Kullanıcı tercihlerini cache'den alır, yoksa veri kaynağından yükler ve cache'e kaydeder
     * @param userId Kullanıcı ID
     * @param loader Veri kaynağından yükleme fonksiyonu
     * @return UserPreference objesi
     */
    public Mono<UserPreference> getUserPreference(String userId, Supplier<Mono<UserPreference>> loader) {
        return getUserPreference(userId, loader, DEFAULT_TTL);
    }

    /**
     * Kullanıcı tercihlerini cache'den alır, yoksa veri kaynağından yükler ve belirtilen süre için cache'e kaydeder
     * @param userId Kullanıcı ID
     * @param loader Veri kaynağından yükleme fonksiyonu
     * @param ttl Cache süresi
     * @return UserPreference objesi
     */
    public Mono<UserPreference> getUserPreference(String userId, Supplier<Mono<UserPreference>> loader, Duration ttl) {
        String key = USER_PREFERENCES_KEY_PREFIX + userId;
        return redisTemplate.opsForValue().get(key)
                .cast(UserPreference.class)
                .doOnSuccess(cached -> {
                    if (cached != null) {
                        log.debug("Kullanıcı tercihleri önbellekte bulundu: userId={}", userId);
                    } else {
                        log.debug("Kullanıcı tercihleri önbellekte bulunamadı: userId={}", userId);
                    }
                })
                .doOnError(error -> log.error("Redis'ten kullanıcı tercihleri alınırken hata: {}", error.getMessage()));
    }

    /**
     * Kullanıcı tercihlerini cache'e kaydeder
     * @param userId Kullanıcı ID
     * @param userPreference UserPreference objesi
     * @return İşlem sonucu
     */
    public Mono<UserPreference> cacheUserPreference(String userId, UserPreference userPreference) {
        return cacheUserPreference(userId, userPreference, DEFAULT_TTL);
    }

    /**
     * Kullanıcı tercihlerini belirtilen süre için cache'e kaydeder
     * @param userId Kullanıcı ID
     * @param userPreference UserPreference objesi
     * @param ttl Cache süresi
     * @return İşlem sonucu
     */
    public Mono<UserPreference> cacheUserPreference(String userId, UserPreference userPreference, Duration ttl) {
        String key = USER_PREFERENCES_KEY_PREFIX + userId;
        return redisTemplate.opsForValue().set(key, userPreference, CACHE_TTL)
                .thenReturn(userPreference)
                .doOnSuccess(p -> log.debug("Kullanıcı tercihleri başarıyla önbelleğe kaydedildi: userId={}", userPreference.getUserId()))
                .doOnError(error -> log.error("Kullanıcı tercihleri önbelleğe kaydedilirken hata: {}", error.getMessage()));
    }

    /**
     * Kullanıcı tercihlerini cache'den siler
     * @param userId Kullanıcı ID
     * @return İşlem sonucu
     */
    public Mono<Void> invalidateUserPreference(String userId) {
        String key = USER_PREFERENCES_KEY_PREFIX + userId;
        return redisTemplate.delete(key)
                .doOnSuccess(deleted -> log.debug("Kullanıcı tercihleri önbellekten silindi: userId={}, deleted={}", userId, deleted))
                .doOnError(error -> log.error("Kullanıcı tercihleri önbellekten silinirken hata: {}", error.getMessage()))
                .then();
    }
    
    /**
     * Kullanıcı tercihlerini Redis'ten alır
     * @param userId Kullanıcı ID
     * @return UserPreference objesi
     */
    public Mono<UserPreference> getUserPreferences(String userId) {
        String key = USER_PREFERENCES_KEY_PREFIX + userId;
        return redisTemplate.opsForValue().get(key)
                .cast(UserPreference.class)
                .doOnSuccess(cached -> {
                    if (cached != null) {
                        log.debug("Kullanıcı tercihleri önbellekte bulundu: userId={}", userId);
                    } else {
                        log.debug("Kullanıcı tercihleri önbellekte bulunamadı: userId={}", userId);
                    }
                })
                .doOnError(error -> log.error("Redis'ten kullanıcı tercihleri alınırken hata: {}", error.getMessage()));
    }
    
    /**
     * Kullanıcı tercihlerini Redis'e kaydeder
     * @param userPreference Kaydedilecek kullanıcı tercihleri
     * @return Kaydedilen kullanıcı tercihleri
     */
    public Mono<UserPreference> saveUserPreferences(UserPreference userPreference) {
        String key = USER_PREFERENCES_KEY_PREFIX + userPreference.getUserId();
        return redisTemplate.opsForValue().set(key, userPreference, CACHE_TTL)
                .thenReturn(userPreference)
                .doOnSuccess(p -> log.debug("Kullanıcı tercihleri başarıyla önbelleğe kaydedildi: userId={}", userPreference.getUserId()))
                .doOnError(error -> log.error("Kullanıcı tercihleri önbelleğe kaydedilirken hata: {}", error.getMessage()));
    }
    
    /**
     * Kullanıcı tercihlerini Redis'ten siler
     * @param userId Kullanıcı ID
     * @return İşlem sonucu
     */
    public Mono<Void> deleteUserPreferences(String userId) {
        String key = USER_PREFERENCES_KEY_PREFIX + userId;
        return redisTemplate.delete(key)
                .then()
                .doOnSuccess(unused -> log.debug("Kullanıcı tercihleri önbellekten silindi: userId={}", userId))
                .doOnError(error -> log.error("Kullanıcı tercihleri önbellekten silinirken hata: {}", error.getMessage()));
    }
}