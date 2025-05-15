package com.craftpilot.userservice.service;

import com.craftpilot.redis.service.ReactiveCacheService;
import com.craftpilot.userservice.model.UserPreference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

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

    private final ReactiveCacheService cacheService;
    
    // Default cache TTL
    private static final Duration DEFAULT_USER_TTL = Duration.ofMinutes(15);
    private static final Duration DEFAULT_PREFERENCE_TTL = Duration.ofMinutes(10);
    
    private static final String USER_CACHE_PREFIX = "user:";
    private static final String PREFERENCE_CACHE_PREFIX = "preference:";

    /**
     * Get a cached user or compute if not found
     */
    public <T> Mono<T> getCachedUser(String userId, Supplier<Mono<T>> supplier) {
        String cacheKey = getUserCacheKey(userId);
        log.debug("Getting cached user with key: {}", cacheKey);
        
        return cacheService.get(cacheKey, (Class<T>)Object.class)
                .switchIfEmpty(Mono.defer(() -> {
                    log.debug("Cache miss for user {}, computing value", userId);
                    return supplier.get()
                            .flatMap(value -> cacheService.put(cacheKey, value, DEFAULT_USER_TTL).thenReturn(value));
                }));
    }

    /**
     * Cache a user object
     */
    public <T> Mono<Boolean> cacheUser(String userId, T user) {
        String cacheKey = getUserCacheKey(userId);
        log.debug("Caching user with key: {}", cacheKey);
        
        return cacheService.put(cacheKey, user, DEFAULT_USER_TTL);
    }

    /**
     * Invalidate user cache
     */
    public Mono<Boolean> invalidateUserCache(String userId) {
        String cacheKey = getUserCacheKey(userId);
        log.debug("Invalidating user cache with key: {}", cacheKey);
        
        return cacheService.invalidate(cacheKey);
    }

    /**
     * Get a cached preference or compute if not found
     */
    public <T> Mono<T> getCachedPreference(String userId, Supplier<Mono<T>> supplier) {
        String cacheKey = getPreferenceCacheKey(userId);
        log.debug("Getting cached preference with key: {}", cacheKey);
        
        return cacheService.get(cacheKey, (Class<T>)Object.class)
                .switchIfEmpty(Mono.defer(() -> {
                    log.debug("Cache miss for preference {}, computing value", userId);
                    return supplier.get()
                            .flatMap(value -> cacheService.put(cacheKey, value, DEFAULT_PREFERENCE_TTL).thenReturn(value));
                }));
    }

    /**
     * Cache a preference object
     */
    public <T> Mono<Boolean> cachePreference(String userId, T preference) {
        String cacheKey = getPreferenceCacheKey(userId);
        log.debug("Caching preference with key: {}", cacheKey);
        
        return cacheService.put(cacheKey, preference, DEFAULT_PREFERENCE_TTL);
    }

    /**
     * Invalidate preference cache
     */
    public Mono<Boolean> invalidatePreferenceCache(String userId) {
        String cacheKey = getPreferenceCacheKey(userId);
        log.debug("Invalidating preference cache with key: {}", cacheKey);
        
        return cacheService.invalidate(cacheKey);
    }

    /**
     * Generic cache-or-compute method
     */
    public <T> Mono<T> getOrCompute(String key, Supplier<Mono<T>> supplier, Duration ttl) {
        log.debug("Getting or computing value for key: {}", key);
        
        return cacheService.get(key, (Class<T>)Object.class)
                .switchIfEmpty(Mono.defer(() -> {
                    log.debug("Cache miss for key {}, computing value", key);
                    return supplier.get()
                            .flatMap(value -> cacheService.put(key, value, ttl).thenReturn(value));
                }));
    }

    /**
     * Get a UserPreference from cache
     */
    public Mono<UserPreference> getUserPreference(String userId) {
        String cacheKey = getPreferenceCacheKey(userId);
        log.debug("Getting user preference from cache with key: {}", cacheKey);
        
        return cacheService.get(cacheKey, UserPreference.class);
    }

    /**
     * Get a UserPreference from cache
     * Method required by UserPreferenceService
     */
    public Mono<UserPreference> getUserPreferences(String userId) {
        String cacheKey = getPreferenceCacheKey(userId);
        log.debug("Getting user preferences from cache with key: {}", cacheKey);
        
        return cacheService.get(cacheKey, UserPreference.class);
    }

    /**
     * Cache a UserPreference
     */
    public Mono<Boolean> cacheUserPreference(String userId, UserPreference preference) {
        String cacheKey = getPreferenceCacheKey(userId);
        log.debug("Caching user preference with key: {}", cacheKey);
        
        return cacheService.put(cacheKey, preference, DEFAULT_PREFERENCE_TTL);
    }

    /**
     * Save UserPreference to cache
     * Method required by UserPreferenceService
     */
    public Mono<Boolean> saveUserPreferences(UserPreference preference) {
        String userId = preference.getUserId();
        String cacheKey = getPreferenceCacheKey(userId);
        log.debug("Saving user preferences to cache with key: {}", cacheKey);
        
        return cacheService.put(cacheKey, preference, DEFAULT_PREFERENCE_TTL);
    }

    /**
     * Delete UserPreference from cache
     * Method required by UserPreferenceService
     */
    public Mono<Boolean> deleteUserPreferences(String userId) {
        String cacheKey = getPreferenceCacheKey(userId);
        log.debug("Deleting user preferences from cache with key: {}", cacheKey);
        
        return cacheService.invalidate(cacheKey);
    }

    /**
     * Check if a UserPreference exists in cache
     */
    public Mono<Boolean> hasUserPreferenceInCache(String userId) {
        String cacheKey = getPreferenceCacheKey(userId);
        
        return cacheService.get(cacheKey, UserPreference.class)
                .map(preference -> true)
                .defaultIfEmpty(false);
    }

    /**
     * Batch invalidate user preferences
     */
    public Mono<Void> batchInvalidatePreferences(String... userIds) {
        if (userIds == null || userIds.length == 0) {
            return Mono.empty();
        }
        
        return Mono.fromRunnable(() -> {
            for (String userId : userIds) {
                invalidatePreferenceCache(userId).subscribe(
                    result -> log.debug("Preference cache invalidated for user {}: {}", userId, result),
                    error -> log.error("Error invalidating preference cache for user {}: {}", userId, error)
                );
            }
        });
    }

    // Helper methods for key generation
    private String getUserCacheKey(String userId) {
        return USER_CACHE_PREFIX + userId;
    }

    private String getPreferenceCacheKey(String userId) {
        return PREFERENCE_CACHE_PREFIX + userId;
    }
}