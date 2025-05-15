package com.craftpilot.redis.health;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.ReactiveHealthIndicator;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import reactor.core.publisher.Mono;

@Slf4j
@RequiredArgsConstructor
public class RedisHealthIndicator implements ReactiveHealthIndicator {

    private final ReactiveRedisConnectionFactory connectionFactory;

    @Override
    public Mono<Health> health() {
        return connectionFactory.getReactiveConnection()
                .ping()
                .map(ping -> {
                    log.debug("Redis health check: PING response = {}", ping);
                    return Health.up()
                            .withDetail("ping", ping)
                            .withDetail("client", "redis-client-lib")
                            .build();
                })
                .onErrorResume(e -> {
                    log.error("Redis health check failed: {}", e.getMessage());
                    return Mono.just(Health.down()
                            .withException(e)
                            .withDetail("client", "redis-client-lib")
                            .build());
                });
    }
}
