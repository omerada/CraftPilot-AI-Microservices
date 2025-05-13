package com.craftpilot.analyticsservice.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.ReactiveHealthIndicator;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
public class MongoHealthIndicator implements ReactiveHealthIndicator {
    
    private final ReactiveMongoTemplate mongoTemplate;
    
    @Override
    public Mono<Health> health() {
        return checkMongoConnection()
            .map(status -> status ? Health.up().build() : Health.down().build())
            .onErrorResume(ex -> {
                log.error("MongoDB health check failed: {}", ex.getMessage());
                return Mono.just(Health.down().withException(ex).build());
            });
    }
    
    private Mono<Boolean> checkMongoConnection() {
        return mongoTemplate.executeCommand("{ ping: 1 }")
            .map(document -> {
                if (document.getDouble("ok") == 1.0) {
                    log.debug("MongoDB health check: Connection is OK");
                    return true;
                } else {
                    log.warn("MongoDB health check: Connection failed");
                    return false;
                }
            })
            .onErrorResume(ex -> {
                log.error("MongoDB health check error: {}", ex.getMessage());
                return Mono.just(false);
            });
    }
}
