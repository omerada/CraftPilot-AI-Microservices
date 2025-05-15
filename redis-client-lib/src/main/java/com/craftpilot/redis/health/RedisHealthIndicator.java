package com.craftpilot.redis.health;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.ReactiveHealthIndicator;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Slf4j
public class RedisHealthIndicator implements ReactiveHealthIndicator {
    private final ReactiveRedisConnectionFactory connectionFactory;
    
    public RedisHealthIndicator(ReactiveRedisConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    @Override
    public Mono<Health> health() {
        return checkRedisConnection()
                .map(response -> Health.up()
                        .withDetail("ping_response", response)
                        .withDetail("client", connectionFactory.getClass().getSimpleName())
                        .build())
                .timeout(Duration.ofSeconds(2))
                .onErrorResume(ex -> {
                    log.warn("Redis health check failed: {}", ex.getMessage());
                    return Mono.just(Health.down()
                            .withDetail("error", ex.getMessage())
                            .withDetail("client", connectionFactory.getClass().getSimpleName())
                            .build());
                });
    }

    private Mono<String> checkRedisConnection() {
        return connectionFactory.getReactiveConnection()
                .ping()
                .doOnSubscribe(s -> log.debug("Performing Redis health check"))
                .doOnNext(pong -> log.debug("Redis health check result: {}", pong))
                .doOnError(e -> log.warn("Redis health check error: {}", e.getMessage()));
    }
}
