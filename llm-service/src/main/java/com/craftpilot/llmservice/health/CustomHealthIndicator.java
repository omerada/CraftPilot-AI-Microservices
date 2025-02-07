package com.craftpilot.llmservice.health;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.ReactiveHealthIndicator;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class CustomHealthIndicator implements ReactiveHealthIndicator {
    private final ReactiveRedisConnectionFactory redisConnectionFactory;
    private final WebClient webClient;

    @Override
    public Mono<Health> health() {
        return checkRedis()
                .then(checkOpenRouter())
                .thenReturn(Health.up().build())
                .onErrorResume(throwable -> Mono.just(Health.builder()
                        .down()
                        .withException(throwable)
                        .build()));
    }

    private Mono<Void> checkRedis() {
        return redisConnectionFactory.getReactiveConnection()
                .ping()
                .then();
    }

    private Mono<Void> checkOpenRouter() {
        return webClient.get()
                .uri("/health")
                .retrieve()
                .bodyToMono(Void.class);
    }
} 