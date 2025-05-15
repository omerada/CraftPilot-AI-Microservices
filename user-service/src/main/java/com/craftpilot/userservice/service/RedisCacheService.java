package com.craftpilot.userservice.service;

import com.craftpilot.userservice.model.UserPreference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import com.craftpilot.redis.service.ReactiveCacheService;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * User service için Redis cache servis wrapper'ı
 * Bu sınıf redis-client-lib'nin ReactiveCacheService sınıfını kullanarak
 * user-service için özel cache işlemlerini gerçekleştirir.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RedisCacheService {
    private final ReactiveCacheService reactiveCacheService;
    private static final Duration DEFAULT_TTL = Duration.ofMinutes(30);
    private static final String USER_PREFERENCE_PREFIX = "user:preference:";

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
        String key = USER_PREFERENCE_PREFIX + userId;
        return reactiveCacheService.getOrCompute(key, UserPreference.class, loader, ttl);
    }

    /**
     * Kullanıcı tercihlerini cache'e kaydeder
     * @param userId Kullanıcı ID
     * @param userPreference UserPreference objesi
     * @return İşlem sonucu
     */
    public Mono<Boolean> cacheUserPreference(String userId, UserPreference userPreference) {
        return cacheUserPreference(userId, userPreference, DEFAULT_TTL);
    }

    /**
     * Kullanıcı tercihlerini belirtilen süre için cache'e kaydeder
     * @param userId Kullanıcı ID
     * @param userPreference UserPreference objesi
     * @param ttl Cache süresi
     * @return İşlem sonucu
     */
    public Mono<Boolean> cacheUserPreference(String userId, UserPreference userPreference, Duration ttl) {
        String key = USER_PREFERENCE_PREFIX + userId;
        return reactiveCacheService.put(key, userPreference, ttl);
    }

    /**
     * Kullanıcı tercihlerini cache'den siler
     * @param userId Kullanıcı ID
     * @return İşlem sonucu
     */
    public Mono<Boolean> invalidateUserPreference(String userId) {
        String key = USER_PREFERENCE_PREFIX + userId;
        return reactiveCacheService.invalidate(key);
    }
}