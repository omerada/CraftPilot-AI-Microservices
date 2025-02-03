package com.craftpilot.aiquestionservice.health;

import com.google.cloud.firestore.Firestore;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.ReactiveHealthIndicator;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class CustomHealthIndicator implements ReactiveHealthIndicator {
    private final Firestore firestore;
    private final ReactiveRedisConnectionFactory redisConnectionFactory;
    private final KafkaTemplate<String, ?> kafkaTemplate;

    @Override
    public Mono<Health> health() {
        return checkFirestore()
                .and(checkRedis())
                .and(checkKafka())
                .map(health -> Health.up()
                        .withDetail("firestore", "UP")
                        .withDetail("redis", "UP")
                        .withDetail("kafka", "UP")
                        .build())
                .onErrorResume(ex -> Mono.just(Health.down()
                        .withException(ex)
                        .build()));
    }

    private Mono<Void> checkFirestore() {
        return Mono.fromCallable(() -> {
            firestore.collection("health-check").document().getId();
            return null;
        });
    }

    private Mono<Void> checkRedis() {
        return redisConnectionFactory.getReactiveConnection()
                .ping()
                .then();
    }

    private Mono<Void> checkKafka() {
        return Mono.fromCallable(() -> {
            kafkaTemplate.metrics();
            return null;
        });
    }
} 