package com.craftpilot.analyticsservice.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.ReactiveHealthIndicator;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class MongoHealthIndicator implements ReactiveHealthIndicator {

    private final ReactiveMongoTemplate mongoTemplate;

    public MongoHealthIndicator(ReactiveMongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Mono<Health> health() {
        return checkMongoConnection()
                .map(status -> status ? Health.up().withDetails(getMongoDetails()).build() 
                                     : Health.down().build())
                .onErrorResume(e -> {
                    log.error("MongoDB health check failed: {}", e.getMessage());
                    Map<String, Object> details = new HashMap<>();
                    details.put("error", e.getMessage());
                    details.put("exception", e.getClass().getName());
                    return Mono.just(Health.down().withDetails(details).build());
                });
    }

    private Mono<Boolean> checkMongoConnection() {
        return mongoTemplate.getMongoDatabase()
                .flatMap(db -> {
                    // Execute ping command to check database connectivity
                    return mongoTemplate.executeCommand("{ ping: 1 }")
                            .map(document -> document.getDouble("ok").intValue() == 1)
                            .onErrorResume(e -> {
                                log.warn("MongoDB ping command failed: {}", e.getMessage());
                                return Mono.just(false);
                            });
                })
                .defaultIfEmpty(false);
    }

    private Map<String, Object> getMongoDetails() {
        Map<String, Object> details = new HashMap<>();
        // Get database name directly from the template instead of from Mono
        details.put("database", mongoTemplate.getMongoDatabase().block().getName());
        return details;
    }
}
