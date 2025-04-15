package com.craftpilot.lighthouseservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.ReactiveHealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Configuration
public class RedisHealthIndicatorConfig {

    @Value("${lighthouse.redis.connection.max-attempts:5}")
    private int maxConnectionAttempts;
    
    @Value("${lighthouse.redis.connection.retry-delay-ms:3000}")
    private long retryDelayMs;

    @Bean
    public ReactiveHealthIndicator redisHealthIndicator(ReactiveRedisConnectionFactory connectionFactory) {
        return () -> checkRedisConnection(connectionFactory)
                    .timeout(Duration.ofMillis(2000))
                    .onErrorResume(e -> Mono.just(Health.down()
                        .withException(e)
                        .withDetail("cause", "Redis connection failed: " + e.getMessage())
                        .build()));
    }
    
    private Mono<Health> checkRedisConnection(ReactiveRedisConnectionFactory factory) {
        return Mono.fromCallable(() -> factory.getReactiveConnection().ping())
                .retry(maxConnectionAttempts)
                .flatMap(ping -> Mono.just(Health.up()
                        .withDetail("connection", "successful")
                        .withDetail("ping", "success")
                        .withDetail("timestamp", System.currentTimeMillis())
                        .build()))
                .onErrorResume(e -> {
                    // Kapsamlı hata bilgisi
                    return Mono.just(Health.down()
                        .withDetail("connection", "failed")
                        .withDetail("error", e.getClass().getSimpleName())
                        .withDetail("message", e.getMessage())
                        .withDetail("timestamp", System.currentTimeMillis())
                        .build());
                })
                .delaySubscription(Duration.ofMillis(retryDelayMs));
    }
}
