package com.craftpilot.userservice.health;

import com.craftpilot.userservice.service.RedisCacheService;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.ReactiveHealthIndicator;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import lombok.extern.slf4j.Slf4j;
import java.time.Duration;

@Component
@Slf4j
public class RedisHealthIndicator implements ReactiveHealthIndicator {

    private final RedisCacheService redisCacheService;

    public RedisHealthIndicator(RedisCacheService redisCacheService) {
        this.redisCacheService = redisCacheService;
    }

    @Override
    public Mono<Health> health() {
        // Return cached health status immediately if Redis is known to be unhealthy
        if (!redisCacheService.isRedisHealthy()) {
            log.warn("Redis health check skipped - known to be unhealthy");
            return Mono.just(Health.down()
                .withDetail("message", "Redis connection is not healthy")
                .withDetail("status", "DOWN")
                .withDetail("source", "cached_status")
                .build());
        }
        
        // Actively ping Redis to verify connection - zaman aşımını 3'ten 1 saniyeye düşürelim
        return redisCacheService.pingRedis()
            .timeout(Duration.ofMillis(1000)) // 3 saniyeden 1 saniyeye düşürüldü
            .map(ping -> {
                if (ping) {
                    log.debug("Redis health check passed");
                    return Health.up()
                        .withDetail("message", "Redis connection is healthy")
                        .withDetail("status", "UP")
                        .withDetail("source", "active_ping")
                        .build();
                } else {
                    log.warn("Redis health check failed - ping returned false");
                    return Health.down()
                        .withDetail("message", "Redis ping failed")
                        .withDetail("status", "DOWN")
                        .withDetail("source", "active_ping")
                        .build();
                }
            })
            .onErrorResume(e -> {
                log.error("Redis health check error: {}", e.getMessage());
                return Mono.just(Health.down()
                    .withDetail("message", "Redis health check failed: " + e.getMessage())
                    .withDetail("status", "DOWN")
                    .withDetail("source", "active_ping_error")
                    .withDetail("exception", e.getClass().getSimpleName())
                    .build());
            });
    }
}
