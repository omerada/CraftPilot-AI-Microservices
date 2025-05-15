package com.craftpilot.redis.service;

import com.craftpilot.redis.exception.RedisOperationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * Reactive Redis Cache Service
 * Cache operasyonları için reactive API sağlar
 */
@Slf4j
public class ReactiveCacheService {

    private final ReactiveRedisTemplate<String, Object> redisTemplate;
    private final Duration defaultTtl;

    public ReactiveCacheService(ReactiveRedisTemplate<String, Object> redisTemplate) {
        this(redisTemplate, Duration.ofMinutes(30));
    }

    public ReactiveCacheService(
            ReactiveRedisTemplate<String, Object> redisTemplate,
            Duration defaultTtl) {
        this.redisTemplate = redisTemplate;
        this.defaultTtl = defaultTtl;
    }

    // Üçüncü bir constructor CircuitBreaker için
    public ReactiveCacheService(
            ReactiveRedisTemplate<String, Object> redisTemplate,
            Object circuitBreakerRegistry,
            Duration defaultTtl) {
        this.redisTemplate = redisTemplate;
        this.defaultTtl = defaultTtl;
    }

    /**
     * Redis'ten bir değer alır
     * @param key Cache key
     * @param clazz Dönüş tipi
     * @return Cache değeri
     */
    public <T> Mono<T> get(String key, Class<T> clazz) {
        return redisTemplate.opsForValue().get(key)
                .cast(clazz)
                .doOnSubscribe(s -> log.debug("Getting value from cache: key={}, type={}", key, clazz.getSimpleName()))
                .doOnSuccess(value -> {
                    if (value != null) {
                        log.debug("Value retrieved from cache: key={}, type={}", key, clazz.getSimpleName());
                    } else {
                        log.debug("Value not found in cache: key={}, type={}", key, clazz.getSimpleName());
                    }
                })
                .doOnError(e -> log.error("Error retrieving from cache: key={}, type={}, error={}", 
                    key, clazz.getSimpleName(), e.getMessage()));
    }

    /**
     * Redis'e bir değer kaydeder
     * @param key Cache key
     * @param value Cache değeri
     * @param ttl Cache timeout
     * @return İşlem sonucu
     */
    public <T> Mono<Boolean> put(String key, T value, Duration ttl) {
        return redisTemplate.opsForValue().set(key, value, ttl)
                .doOnSubscribe(s -> log.debug("Cache'e değer kaydediliyor: key={}, ttl={}", key, ttl))
                .doOnSuccess(result -> log.debug("Cache'e değer kaydedildi: key={}, success={}", key, result))
                .doOnError(e -> log.error("Cache'e değer kaydedilirken hata: key={}, error={}", key, e.getMessage()))
                .onErrorMap(e -> new RedisOperationException("Cache write failed for key: " + key, e));
    }

    /**
     * Redis'ten bir değeri siler
     * @param key Cache key
     * @return İşlem sonucu
     */
    public Mono<Boolean> invalidate(String key) {
        return redisTemplate.delete(key)
                .map(count -> count > 0)
                .doOnSubscribe(s -> log.debug("Cache değeri siliniyor: key={}", key))
                .doOnSuccess(result -> log.debug("Cache değeri silindi: key={}, success={}", key, result))
                .doOnError(e -> log.error("Cache değeri silinirken hata: key={}, error={}", key, e.getMessage()))
                .onErrorMap(e -> new RedisOperationException("Cache invalidation failed for key: " + key, e));
    }

    /**
     * Cache'te değer varsa alır, yoksa hesaplar ve cache'e kaydeder
     * @param key Cache key
     * @param supplier Değer sağlayıcı fonksiyon
     * @param ttl Cache timeout
     * @return Cache değeri veya hesaplanan değer
     */
    @SuppressWarnings("unchecked")
    public <T> Mono<T> getOrCompute(String key, Supplier<Mono<T>> supplier, Duration ttl) {
        return get(key, (Class<T>) Object.class)
                .switchIfEmpty(Mono.defer(() -> {
                    log.debug("Cache'te değer bulunamadı, hesaplanıyor: key={}", key);
                    return supplier.get()
                            .flatMap(value -> put(key, value, ttl).thenReturn(value))
                            .doOnSuccess(value -> log.debug("Değer hesaplandı ve cache'e kaydedildi: key={}", key));
                }));
    }

    /**
     * Cache'te değer varsa alır, yoksa hesaplar ve varsayılan TTL ile cache'e kaydeder
     * @param key Cache key
     * @param supplier Değer sağlayıcı fonksiyon
     * @return Cache değeri veya hesaplanan değer
     */
    public <T> Mono<T> getOrCompute(String key, Supplier<Mono<T>> supplier) {
        return getOrCompute(key, supplier, defaultTtl);
    }

    /**
     * Cache'te değer varsa alır, yoksa hesaplar ve cache'e kaydeder
     * @param key Cache key
     * @param clazz Value class type
     * @param supplier Değer sağlayıcı fonksiyon
     * @param ttl Cache timeout
     * @return Cache değeri veya hesaplanan değer
     */
    public <T> Mono<T> getOrCompute(String key, Class<T> clazz, Supplier<Mono<T>> supplier, Duration ttl) {
        return get(key, clazz)
                .switchIfEmpty(Mono.defer(() -> {
                    log.debug("Cache'te değer bulunamadı, hesaplanıyor: key={}", key);
                    return supplier.get()
                            .flatMap(value -> put(key, value, ttl).thenReturn(value))
                            .doOnSuccess(value -> log.debug("Değer hesaplandı ve cache'e kaydedildi: key={}", key));
                }));
    }

    /**
     * Cache'te değer varsa alır, yoksa hesaplar ve varsayılan TTL ile cache'e kaydeder
     * @param key Cache key
     * @param clazz Value class type
     * @param supplier Değer sağlayıcı fonksiyon 
     * @return Cache değeri veya hesaplanan değer
     */
    public <T> Mono<T> getOrCompute(String key, Class<T> clazz, Supplier<Mono<T>> supplier) {
        return getOrCompute(key, clazz, supplier, defaultTtl);
    }
}
