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
                .map(result -> {
                    if (result.getDouble("ok") == 1.0) {
                        // Get database name safely
                        String dbName = mongoTemplate.getMongoDatabase().block().getName();
                        return Health.up()
                                .withDetail("database", dbName)
                                .withDetail("status", "MongoDB bağlantısı sağlıklı")
                                .build();
                    } else {
                        // Get database name safely
                        String dbName = mongoTemplate.getMongoDatabase().block().getName();
                        return Health.down()
                                .withDetail("database", dbName)
                                .withDetail("error", "MongoDB sunucusu ping yanıtında hata döndü")
                                .build();
                    }
                })
                .onErrorResume(e -> Mono.just(Health.down()
                        .withDetail("error", "MongoDB bağlantı hatası: " + e.getMessage())
                        .build()));
    }
}
