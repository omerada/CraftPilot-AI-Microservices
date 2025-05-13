package com.craftpilot.analyticsservice.config;

import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.ReactiveHealthIndicator;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class MongoHealthIndicator implements ReactiveHealthIndicator {

    private final ReactiveMongoTemplate mongoTemplate;
    private final String databaseName;

    public MongoHealthIndicator(ReactiveMongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
        this.databaseName = mongoTemplate.getMongoDatabase()
                .map(db -> db.getName())
                .block(); // Safe to block during initialization
    }

    @Override
    public Mono<Health> health() {
        return checkMongoHealth()
                .timeout(Duration.ofSeconds(5))
                .onErrorResume(e -> {
                    log.error("MongoDB sağlık kontrolü başarısız: {}", e.getMessage());
                    Map<String, Object> details = new HashMap<>();
                    details.put("error", e.getMessage());
                    details.put("exception", e.getClass().getName());
                    return Mono.just(Health.down().withDetails(details).build());
                });
    }

    private Mono<Health> checkMongoHealth() {
        return mongoTemplate.executeCommand(new Document("ping", 1))
                .map(result -> {
                    Boolean ok = result.getDouble("ok").intValue() == 1;
                    if (ok) {
                        log.debug("MongoDB sağlık kontrolü başarılı");
                        Map<String, Object> details = new HashMap<>();
                        details.put("ping", "ok");
                        details.put("database", databaseName);
                        return Health.up().withDetails(details).build();
                    } else {
                        log.warn("MongoDB ping komutu başarısız");
                        return Health.down().withDetail("ping", "failed").build();
                    }
                });
    }
}
