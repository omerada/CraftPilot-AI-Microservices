package com.craftpilot.lighthouseservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/health")
@RequiredArgsConstructor
public class HealthController {

    private final RedisConnectionFactory redisConnectionFactory;
    private final RedisHealthIndicator redisHealthIndicator;

    @GetMapping
    public Mono<ResponseEntity<Map<String, Object>>> healthCheck() {
        return Mono.fromCallable(() -> {
            Health health = redisHealthIndicator.health();
            boolean isUp = "UP".equals(health.getStatus().getCode());
            
            Map<String, Object> status = Map.of(
                "status", isUp ? "UP" : "DOWN",
                "redis", isUp ? "UP" : "DOWN",
                "timestamp", System.currentTimeMillis(),
                "details", health.getDetails()
            );
            
            return ResponseEntity.ok(status);
        });
    }
    
    @Component
    public class RedisHealthIndicator implements HealthIndicator {
        @Override
        public Health health() {
            try {
                boolean redisConnected = redisConnectionFactory.getConnection().ping() != null;
                if (redisConnected) {
                    return Health.up()
                        .withDetail("connection", "successful")
                        .build();
                } else {
                    return Health.down()
                        .withDetail("connection", "failed")
                        .build();
                }
            } catch (Exception e) {
                return Health.down(e)
                    .withDetail("connection", "error")
                    .withDetail("error", e.getMessage())
                    .build();
            }
        }
    }
}
