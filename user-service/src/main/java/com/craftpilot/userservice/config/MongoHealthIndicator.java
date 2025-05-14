package com.craftpilot.userservice.config;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.ReactiveHealthIndicator;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Component
public class MongoHealthIndicator implements ReactiveHealthIndicator {

    private final ReactiveMongoTemplate mongoTemplate;

    public MongoHealthIndicator(ReactiveMongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Mono<Health> health() {
        return checkMongoConnection()
                .timeout(Duration.ofSeconds(5))
                .onErrorResume(e -> Mono.just(Health.down()
                        .withException(e)
                        .withDetail("cause", "MongoDB connection failed: " + e.getMessage())
                        .build()));
    }

    private Mono<Health> checkMongoConnection() {
        return mongoTemplate.executeCommand("{ ping: 1 }")
                .map(document -> Health.up()
                        .withDetail("ping", "successful")
                        .withDetail("version", document.get("version", "unknown"))
                        .withDetail("timestamp", System.currentTimeMillis())
                        .build())
                .onErrorResume(e -> Mono.just(Health.down()
                        .withDetail("ping", "failed")
                        .withDetail("error", e.getClass().getSimpleName())
                        .withDetail("message", e.getMessage())
                        .withDetail("timestamp", System.currentTimeMillis())
                        .build()));
    }
}
