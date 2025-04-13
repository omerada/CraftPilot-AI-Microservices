package com.craftpilot.userservice.service;

import com.craftpilot.userservice.model.UserPreference;
import com.craftpilot.userservice.model.user.entity.UserEntity;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@Slf4j
public class RedisCacheService {
    private final ReactiveRedisTemplate<String, UserEntity> userRedisTemplate;
    private final ReactiveRedisTemplate<String, UserPreference> preferenceRedisTemplate;
    private final ReactiveRedisConnectionFactory redisConnectionFactory;
    private final MeterRegistry meterRegistry;
    
    private static final String USER_KEY_PREFIX = "user:";
    private static final String PREFERENCE_KEY_PREFIX = "preference:";
    
    @Value("${redis.cache.ttl.hours:24}")
    private long cacheHours;
    
    private final AtomicBoolean redisHealthy = new AtomicBoolean(true);
    private final Timer redisGetTimer;
    private final Timer redisSetTimer;
    private final Timer redisDeleteTimer;

    // Constructor: @RequiredArgsConstructor yerine açık bir constructor tanımlıyoruz
    public RedisCacheService(
            ReactiveRedisTemplate<String, UserEntity> userRedisTemplate,
            ReactiveRedisTemplate<String, UserPreference> preferenceRedisTemplate,
            ReactiveRedisConnectionFactory redisConnectionFactory,
            MeterRegistry meterRegistry) {
        this.userRedisTemplate = userRedisTemplate;
        this.preferenceRedisTemplate = preferenceRedisTemplate;
        this.redisConnectionFactory = redisConnectionFactory;
        this.meterRegistry = meterRegistry;
        
        // Timer'ları constructor içinde başlatıyoruz
        this.redisGetTimer = Timer.builder("redis.operation.get").register(meterRegistry);
        this.redisSetTimer = Timer.builder("redis.operation.set").register(meterRegistry);
        this.redisDeleteTimer = Timer.builder("redis.operation.delete").register(meterRegistry);
    }

    // Generic methods for UserEntity
    public Mono<UserEntity> get(String key) {
        log.debug("Redis'den veri getiriliyor: key={}", key);
        return redisGetTimer.record(() -> userRedisTemplate.opsForValue().get(USER_KEY_PREFIX + key)
                .doOnSuccess(value -> {
                    if (value != null) {
                        log.debug("Redis'den veri başarıyla getirildi: key={}", key);
                        meterRegistry.counter("redis.hit").increment();
                    } else {
                        log.debug("Redis'de veri bulunamadı: key={}", key);
                        meterRegistry.counter("redis.miss").increment();
                    }
                })
                .doOnError(e -> {
                    log.error("Redis'den veri getirilirken hata: key={}, error={}", key, e.getMessage());
                    meterRegistry.counter("redis.error").increment();
                    if (e instanceof RedisConnectionFailureException) {
                        redisHealthy.set(false);
                    }
                })
                .onErrorResume(e -> Mono.empty()));
    }

    public Mono<Boolean> set(String key, UserEntity value) {
        log.debug("Redis'e veri kaydediliyor: key={}", key);
        return redisSetTimer.record(() -> userRedisTemplate.opsForValue()
                .set(USER_KEY_PREFIX + key, value, Duration.ofHours(cacheHours))
                .doOnSuccess(result -> {
                    log.debug("Redis'e veri başarıyla kaydedildi: key={}", key);
                    meterRegistry.counter("redis.write.success").increment();
                    redisHealthy.set(true);
                })
                .doOnError(e -> {
                    log.error("Redis'e veri kaydedilirken hata: key={}, error={}", key, e.getMessage());
                    meterRegistry.counter("redis.write.error").increment();
                    if (e instanceof RedisConnectionFailureException) {
                        redisHealthy.set(false);
                    }
                })
                .onErrorReturn(false));
    }

    public Mono<Boolean> delete(String key) {
        log.debug("Redis'den veri siliniyor: key={}", key);
        return redisDeleteTimer.record(() -> userRedisTemplate.opsForValue().delete(USER_KEY_PREFIX + key)
                .doOnSuccess(result -> {
                    log.debug("Redis'den veri başarıyla silindi: key={}", key);
                    meterRegistry.counter("redis.delete.success").increment();
                })
                .doOnError(e -> {
                    log.error("Redis'den veri silinirken hata: key={}, error={}", key, e.getMessage());
                    meterRegistry.counter("redis.delete.error").increment();
                    if (e instanceof RedisConnectionFailureException) {
                        redisHealthy.set(false);
                    }
                })
                .onErrorReturn(false));
    }

    // Preference specific methods
    public Mono<UserPreference> getUserPreferences(String userId) {
        log.debug("Kullanıcı tercihleri getiriliyor: userId={}", userId);
        return redisGetTimer.record(() -> preferenceRedisTemplate.opsForValue().get(PREFERENCE_KEY_PREFIX + userId)
                .doOnSuccess(pref -> {
                    if (pref != null) {
                        log.debug("Kullanıcı tercihleri Redis'den getirildi: userId={}", userId);
                        meterRegistry.counter("redis.preference.hit").increment();
                    } else {
                        log.debug("Kullanıcı tercihleri Redis'de bulunamadı: userId={}", userId);
                        meterRegistry.counter("redis.preference.miss").increment();
                    }
                })
                .doOnError(e -> {
                    log.error("Kullanıcı tercihleri getirilirken Redis hatası: userId={}, error={}", userId, e.getMessage());
                    meterRegistry.counter("redis.preference.error").increment();
                    if (e instanceof RedisConnectionFailureException) {
                        redisHealthy.set(false);
                    }
                })
                .onErrorResume(e -> {
                    // Varsayılan tercihleri oluştur
                    UserPreference defaultPref = UserPreference.builder()
                            .userId(userId)
                            .theme("light")
                            .language("tr")
                            .notifications(true)
                            .pushEnabled(true)
                            .createdAt(System.currentTimeMillis())
                            .updatedAt(System.currentTimeMillis())
                            .build();
                    log.warn("Redis hatası nedeniyle varsayılan tercihler döndürülüyor: userId={}", userId);
                    return Mono.just(defaultPref);
                }));
    }

    public Mono<Boolean> saveUserPreferences(UserPreference preference) {
        String userId = preference.getUserId();
        log.debug("Kullanıcı tercihleri Redis'e kaydediliyor: userId={}", userId);
        return redisSetTimer.record(() -> preferenceRedisTemplate.opsForValue()
                .set(PREFERENCE_KEY_PREFIX + userId, preference, Duration.ofHours(cacheHours))
                .doOnSuccess(result -> {
                    log.debug("Kullanıcı tercihleri Redis'e başarıyla kaydedildi: userId={}", userId);
                    meterRegistry.counter("redis.preference.write.success").increment();
                    redisHealthy.set(true);
                })
                .doOnError(e -> {
                    log.error("Kullanıcı tercihleri Redis'e kaydedilirken hata: userId={}, error={}", userId, e.getMessage());
                    meterRegistry.counter("redis.preference.write.error").increment();
                    if (e instanceof RedisConnectionFailureException) {
                        redisHealthy.set(false);
                    }
                })
                .onErrorReturn(false));
    }

    public Mono<Boolean> deleteUserPreferences(String userId) {
        log.debug("Kullanıcı tercihleri Redis'den siliniyor: userId={}", userId);
        return redisDeleteTimer.record(() -> preferenceRedisTemplate.opsForValue().delete(PREFERENCE_KEY_PREFIX + userId)
                .doOnSuccess(result -> {
                    log.debug("Kullanıcı tercihleri Redis'den başarıyla silindi: userId={}", userId);
                    meterRegistry.counter("redis.preference.delete.success").increment();
                })
                .doOnError(e -> {
                    log.error("Kullanıcı tercihleri Redis'den silinirken hata: userId={}, error={}", userId, e.getMessage());
                    meterRegistry.counter("redis.preference.delete.error").increment();
                    if (e instanceof RedisConnectionFailureException) {
                        redisHealthy.set(false);
                    }
                })
                .onErrorReturn(false));
    }
    
    // Redis sağlık kontrolü - her 30 saniyede bir çalışır
    @Scheduled(fixedRate = 30000)
    public void checkRedisHealth() {
        log.debug("Redis sağlık kontrolü yapılıyor...");
        redisConnectionFactory.getReactiveConnection()
            .ping()
            .doOnSuccess(pong -> {
                boolean wasHealthy = redisHealthy.getAndSet(true);
                if (!wasHealthy) {
                    log.info("Redis bağlantısı yeniden sağlandı");
                    meterRegistry.counter("redis.health.recovered").increment();
                }
                meterRegistry.counter("redis.health.check.success").increment();
            })
            .doOnError(e -> {
                log.error("Redis sağlık kontrolü başarısız: {}", e.getMessage());
                redisHealthy.set(false);
                meterRegistry.counter("redis.health.check.failed").increment();
            })
            .subscribe();
    }
    
    // Redis durumunu döndüren metot
    public boolean isRedisHealthy() {
        return redisHealthy.get();
    }
}