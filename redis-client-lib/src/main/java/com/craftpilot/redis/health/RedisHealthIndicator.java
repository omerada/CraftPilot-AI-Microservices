package com.craftpilot.redis.health;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.ReactiveHealthIndicator;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import reactor.core.publisher.Mono;

@Slf4j
public class RedisHealthIndicator implements ReactiveHealthIndicator {

    private final ReactiveRedisConnectionFactory connectionFactory;
    private final String serviceName;

    // Constructor with @Qualifier to ensure the correct bean is injected
    public RedisHealthIndicator(@Qualifier("craftPilotReactiveRedisConnectionFactory") ReactiveRedisConnectionFactory connectionFactory) {
        this(connectionFactory, "redis-client-lib");
    }
    
    // Constructor with service name customization option
    public RedisHealthIndicator(ReactiveRedisConnectionFactory connectionFactory, String serviceName) {
        this.connectionFactory = connectionFactory;
        this.serviceName = serviceName;
    }

    @Override
    public Mono<Health> health() {
        return connectionFactory.getReactiveConnection()
                .ping()
                .map(ping -> {
                    log.debug("Redis health check: PING response = {}", ping);
                    return Health.up()
                            .withDetail("ping", ping)
                            .withDetail("service", serviceName)
                            .build();
                })
                .onErrorResume(e -> {
                    log.error("Redis health check failed: {}", e.getMessage());
                    return Mono.just(Health.down()
                            .withException(e)
                            .withDetail("service", serviceName)
                            .build());
                });
    }
}
