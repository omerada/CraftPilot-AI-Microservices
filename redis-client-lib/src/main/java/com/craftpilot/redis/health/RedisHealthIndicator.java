package com.craftpilot.redis.health;

import com.craftpilot.redis.service.ReactiveRedisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.ReactiveHealthIndicator;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Slf4j
public class RedisHealthIndicator implements ReactiveHealthIndicator {

    private final ReactiveRedisService redisService;

    public RedisHealthIndicator(ReactiveRedisService redisService) {
        this.redisService = redisService;
    }

    @Override
    public Mono<Health> health() {
        // Hızlı dönüş için önbelleklenmiş sağlık durumunu kontrol et
        if (!redisService.isRedisHealthy()) {
            log.warn("Redis sağlık kontrolü atlandı - sağlıksız olduğu biliniyor");
            return Mono.just(Health.down()
                .withDetail("message", "Redis bağlantısı sağlıklı değil")
                .withDetail("status", "DOWN")
                .withDetail("source", "cached_status")
                .build());
        }
        
        // Redis'i aktif olarak yokla
        return redisService.ping()
            .timeout(Duration.ofMillis(1000))
            .map(ping -> {
                if (ping) {
                    log.debug("Redis sağlık kontrolü başarılı");
                    return Health.up()
                        .withDetail("message", "Redis bağlantısı sağlıklı")
                        .withDetail("status", "UP")
                        .withDetail("source", "active_ping")
                        .build();
                } else {
                    log.warn("Redis sağlık kontrolü başarısız - ping false döndü");
                    return Health.down()
                        .withDetail("message", "Redis ping başarısız")
                        .withDetail("status", "DOWN")
                        .withDetail("source", "active_ping")
                        .build();
                }
            })
            .onErrorResume(e -> {
                log.error("Redis sağlık kontrolü hatası: {}", e.getMessage());
                return Mono.just(Health.down()
                    .withDetail("message", "Redis sağlık kontrolü başarısız: " + e.getMessage())
                    .withDetail("status", "DOWN")
                    .withDetail("source", "active_ping_error")
                    .withDetail("exception", e.getClass().getSimpleName())
                    .build());
            });
    }
}
