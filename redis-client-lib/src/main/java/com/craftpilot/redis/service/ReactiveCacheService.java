package com.craftpilot.redis.service;

import com.craftpilot.redis.exception.RedisOperationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Optional;
import java.util.function.Supplier;

@Slf4j
public class ReactiveCacheService extends ReactiveRedisService {

    private final ObjectMapper objectMapper;
    private static final String NULL_VALUE = "@@NULL@@";

    public ReactiveCacheService(
            ReactiveStringRedisTemplate redisTemplate,
            CircuitBreakerRegistry circuitBreakerRegistry,
            Duration defaultTtl,
            ObjectMapper objectMapper) {
        super(redisTemplate, circuitBreakerRegistry, defaultTtl);
        this.objectMapper = objectMapper;
    }

    /**
     * Nesneyi önbellekten alır, yoksa sağlayıcı fonksiyonu çalıştırır
     *
     * @param key Önbellek anahtarı
     * @param supplier Önbellekte yoksa değer üretecek fonksiyon
     * @param type Nesne türü
     * @param <T> Nesne tipi
     * @return Nesne ya da boş Mono
     */
    public <T> Mono<T> getOrCache(String key, Supplier<Mono<T>> supplier, Class<T> type) {
        return getOrCache(key, supplier, type, defaultTtl);
    }

    /**
     * Nesneyi önbellekten alır, yoksa sağlayıcı fonksiyonu çalıştırır
     *
     * @param key Önbellek anahtarı
     * @param supplier Önbellekte yoksa değer üretecek fonksiyon
     * @param type Nesne türü
     * @param ttl Önbellek süresi
     * @param <T> Nesne tipi
     * @return Nesne ya da boş Mono
     */
    public <T> Mono<T> getOrCache(String key, Supplier<Mono<T>> supplier, Class<T> type, Duration ttl) {
        log.debug("Önbellekten veri alınıyor veya sağlayıcı çalıştırılıyor: key={}", key);
        
        return get(key)
            .flatMap(cachedValue -> {
                if (NULL_VALUE.equals(cachedValue)) {
                    log.debug("Önbellekte null değer bulundu: key={}", key);
                    return Mono.empty();
                }
                
                try {
                    T value = objectMapper.readValue(cachedValue, type);
                    log.debug("Önbellekten veri alındı: key={}", key);
                    return Mono.justOrEmpty(value);
                } catch (JsonProcessingException e) {
                    log.error("Önbellekteki veri dönüştürülemedi: key={}, error={}", key, e.getMessage());
                    return delete(key).then(Mono.empty());
                }
            })
            .switchIfEmpty(Mono.defer(() -> {
                log.debug("Önbellekte veri bulunamadı, sağlayıcı çalıştırılıyor: key={}", key);
                return supplier.get()
                    .flatMap(value -> {
                        if (value == null) {
                            return set(key, NULL_VALUE, ttl).thenReturn(null);
                        }
                        
                        try {
                            String json = objectMapper.writeValueAsString(value);
                            return set(key, json, ttl).thenReturn(value);
                        } catch (JsonProcessingException e) {
                            log.error("Nesne JSON'a dönüştürülemedi: key={}, error={}", key, e.getMessage());
                            return Mono.just(value);
                        }
                    });
            }));
    }

    /**
     * Nesneyi önbelleğe kaydeder
     *
     * @param key Önbellek anahtarı
     * @param value Kaydedilecek nesne
     * @param <T> Nesne tipi
     * @return İşlem sonucu
     */
    public <T> Mono<Boolean> cache(String key, T value) {
        return cache(key, value, defaultTtl);
    }

    /**
     * Nesneyi belirli bir TTL ile önbelleğe kaydeder
     *
     * @param key Önbellek anahtarı
     * @param value Kaydedilecek nesne
     * @param ttl Önbellek süresi
     * @param <T> Nesne tipi
     * @return İşlem sonucu
     */
    public <T> Mono<Boolean> cache(String key, T value, Duration ttl) {
        if (value == null) {
            return set(key, NULL_VALUE, ttl);
        }
        
        try {
            String json = objectMapper.writeValueAsString(value);
            return set(key, json, ttl);
        } catch (JsonProcessingException e) {
            log.error("Nesne JSON'a dönüştürülemedi: key={}, error={}", key, e.getMessage());
            return Mono.error(new RedisOperationException("JSON dönüştürme hatası", e));
        }
    }

    /**
     * Önbellekten nesneyi getirir
     *
     * @param key Önbellek anahtarı
     * @param type Nesne türü
     * @param <T> Nesne tipi
     * @return Nesne ya da boş Optional
     */
    public <T> Mono<Optional<T>> getFromCache(String key, Class<T> type) {
        return get(key)
            .flatMap(cachedValue -> {
                if (NULL_VALUE.equals(cachedValue)) {
                    return Mono.just(Optional.<T>empty());
                }
                
                try {
                    T value = objectMapper.readValue(cachedValue, type);
                    return Mono.just(Optional.ofNullable(value));
                } catch (JsonProcessingException e) {
                    log.error("Önbellekteki veri dönüştürülemedi: key={}, error={}", key, e.getMessage());
                    return delete(key).then(Mono.just(Optional.<T>empty()));
                }
            })
            .defaultIfEmpty(Optional.empty());
    }
}
