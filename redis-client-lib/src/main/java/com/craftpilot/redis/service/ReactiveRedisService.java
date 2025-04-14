package com.craftpilot.redis.service;

import com.craftpilot.redis.exception.RedisOperationException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class ReactiveRedisService {

    protected final ReactiveStringRedisTemplate redisTemplate;
    protected final CircuitBreaker circuitBreaker;
    protected final Duration defaultTtl;
    protected final AtomicBoolean redisHealthy = new AtomicBoolean(true);
    private static final String CIRCUIT_BREAKER_NAME = "redisCircuitBreaker"; // Sabit bir isim kullanıyoruz

    public ReactiveRedisService(
            ReactiveStringRedisTemplate redisTemplate,
            CircuitBreakerRegistry circuitBreakerRegistry,
            Duration defaultTtl) {
        this.redisTemplate = redisTemplate;
        // Burada sabit bir isim kullanıyoruz ve bu sayede çakışmaları önlüyoruz
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker(CIRCUIT_BREAKER_NAME);
        this.defaultTtl = defaultTtl;
        
        // Devre kesici durumu değişim olaylarını dinle
        circuitBreaker.getEventPublisher()
            .onStateTransition(event -> {
                if (event.getStateTransition().getToState() == CircuitBreaker.State.OPEN) {
                    log.warn("Redis devre kesici AÇIK durumuna geçti");
                    redisHealthy.set(false);
                } else if (event.getStateTransition().getToState() == CircuitBreaker.State.CLOSED) {
                    log.info("Redis devre kesici KAPALI durumuna döndü");
                    redisHealthy.set(true);
                }
            });
    }

    /**
     * Redis'e değer kaydet
     *
     * @param key Anahtar
     * @param value Değer
     * @return İşlem sonucu
     */
    public Mono<Boolean> set(String key, String value) {
        return set(key, value, defaultTtl);
    }

    /**
     * Redis'e belirli bir TTL ile değer kaydet
     *
     * @param key Anahtar
     * @param value Değer
     * @param ttl TTL süresi
     * @return İşlem sonucu
     */
    public Mono<Boolean> set(String key, String value, Duration ttl) {
        log.debug("Redis'e değer kaydediliyor: key={}", key);
        
        return Mono.defer(() -> redisTemplate.opsForValue().set(key, value, ttl))
                .transform(CircuitBreakerOperator.of(circuitBreaker))
                .doOnSuccess(result -> {
                    if (Boolean.TRUE.equals(result)) {
                        log.debug("Redis'e değer başarıyla kaydedildi: key={}", key);
                    } else {
                        log.warn("Redis'e değer kaydedilemedi: key={}", key);
                    }
                })
                .doOnError(e -> {
                    log.error("Redis'e değer kaydedilirken hata: key={}, error={}", key, e.getMessage());
                    redisHealthy.set(false);
                })
                .onErrorResume(e -> {
                    return Mono.error(new RedisOperationException("Redis veri kaydetme hatası", e));
                });
    }

    /**
     * Redis'ten değer al
     *
     * @param key Anahtar
     * @return Bulunan değer ya da boş Mono
     */
    public Mono<String> get(String key) {
        log.debug("Redis'ten değer alınıyor: key={}", key);
        
        return Mono.defer(() -> redisTemplate.opsForValue().get(key))
                .transform(CircuitBreakerOperator.of(circuitBreaker))
                .doOnSuccess(result -> {
                    if (result != null) {
                        log.debug("Redis'ten değer başarıyla alındı: key={}", key);
                    } else {
                        log.debug("Redis'te değer bulunamadı: key={}", key);
                    }
                })
                .doOnError(e -> {
                    log.error("Redis'ten değer alınırken hata: key={}, error={}", key, e.getMessage());
                    redisHealthy.set(false);
                })
                .onErrorResume(e -> {
                    return Mono.error(new RedisOperationException("Redis veri alma hatası", e));
                });
    }

    /**
     * Redis'ten değer sil
     *
     * @param key Anahtar
     * @return İşlem sonucu
     */
    public Mono<Boolean> delete(String key) {
        log.debug("Redis'ten değer siliniyor: key={}", key);
        
        return Mono.defer(() -> redisTemplate.opsForValue().delete(key))
                .transform(CircuitBreakerOperator.of(circuitBreaker))
                .doOnSuccess(result -> {
                    if (Boolean.TRUE.equals(result)) {
                        log.debug("Redis'ten değer başarıyla silindi: key={}", key);
                    } else {
                        log.debug("Redis'ten silinecek değer bulunamadı: key={}", key);
                    }
                })
                .doOnError(e -> {
                    log.error("Redis'ten değer silinirken hata: key={}, error={}", key, e.getMessage());
                    redisHealthy.set(false);
                })
                .onErrorResume(e -> {
                    return Mono.error(new RedisOperationException("Redis veri silme hatası", e));
                });
    }

    /**
     * Anahtarın varolup olmadığını kontrol et
     *
     * @param key Anahtar
     * @return Anahtar varsa true, yoksa false
     */
    public Mono<Boolean> exists(String key) {
        return Mono.defer(() -> redisTemplate.hasKey(key))
                .transform(CircuitBreakerOperator.of(circuitBreaker))
                .onErrorResume(e -> {
                    log.error("Redis anahtar kontrolünde hata: key={}, error={}", key, e.getMessage());
                    return Mono.error(new RedisOperationException("Redis anahtar kontrolü hatası", e));
                });
    }

    /**
     * Anahtarın süresini belirle
     *
     * @param key Anahtar
     * @param ttl TTL süresi
     * @return İşlem sonucu
     */
    public Mono<Boolean> expire(String key, Duration ttl) {
        return Mono.defer(() -> redisTemplate.expire(key, ttl))
                .transform(CircuitBreakerOperator.of(circuitBreaker))
                .onErrorResume(e -> {
                    log.error("Redis süre belirleme hatası: key={}, error={}", key, e.getMessage());
                    return Mono.error(new RedisOperationException("Redis süre belirleme hatası", e));
                });
    }

    /**
     * Bir anahtarın süresini sorgula
     *
     * @param key Anahtar
     * @return Kalan süre (saniye)
     */
    public Mono<Long> ttl(String key) {
        return Mono.defer(() -> redisTemplate.getExpire(key))
                .transform(CircuitBreakerOperator.of(circuitBreaker))
                .map(duration -> {
                    if (duration == null) {
                        return -1L;
                    }
                    return duration.getSeconds();
                })
                .onErrorResume(e -> {
                    log.error("Redis TTL sorgulama hatası: key={}, error={}", key, e.getMessage());
                    return Mono.error(new RedisOperationException("Redis TTL sorgulama hatası", e));
                });
    }

    /**
     * Redis bağlantısını kontrol et
     *
     * @return Redis sağlıklıysa true, değilse false
     */
    public Mono<Boolean> ping() {
        return Mono.defer(() -> redisTemplate.getConnectionFactory().getReactiveConnection().ping()
                .map(pong -> "PONG".equalsIgnoreCase(pong)))
                .doOnSuccess(result -> {
                    if (Boolean.TRUE.equals(result)) {
                        redisHealthy.set(true);
                    } else {
                        redisHealthy.set(false);
                    }
                })
                .doOnError(e -> {
                    log.error("Redis ping hatası: {}", e.getMessage());
                    redisHealthy.set(false);
                })
                .onErrorReturn(false);
    }

    /**
     * Redis'in sağlıklı olup olmadığını kontrol et
     *
     * @return Redis sağlıklıysa true, değilse false
     */
    public boolean isRedisHealthy() {
        return redisHealthy.get();
    }
}
