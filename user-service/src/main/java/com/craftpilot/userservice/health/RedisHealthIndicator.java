package com.craftpilot.userservice.health;

import com.craftpilot.userservice.service.RedisCacheService;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.ReactiveHealthIndicator;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class RedisHealthIndicator implements ReactiveHealthIndicator {

    private final RedisCacheService redisCacheService;

    public RedisHealthIndicator(RedisCacheService redisCacheService) {
        this.redisCacheService = redisCacheService;
    }

    @Override
    public Mono<Health> health() {
        if (redisCacheService.isRedisHealthy()) {
            return Mono.just(Health.up().withDetail("message", "Redis connection is healthy").build());
        } else {
            return Mono.just(Health.down().withDetail("message", "Redis connection is not healthy").build());
        }
    }
}
