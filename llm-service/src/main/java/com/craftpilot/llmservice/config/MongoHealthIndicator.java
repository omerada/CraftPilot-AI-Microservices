package com.craftpilot.llmservice.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.ReactiveHealthIndicator;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@Slf4j
@RequiredArgsConstructor
public class MongoHealthIndicator implements ReactiveHealthIndicator {

    private final ReactiveMongoTemplate mongoTemplate;

    @Override
    public Mono<Health> health() {
        return checkMongoConnection()
                .map(status -> status ? Health.up().build() : Health.down().build())
                .onErrorResume(exception -> {
                    log.error("MongoDB sağlık kontrolü başarısız: {}", exception.getMessage());
                    return Mono.just(Health.down().withDetail("error", exception.getMessage()).build());
                });
    }

    private Mono<Boolean> checkMongoConnection() {
        return mongoTemplate.executeCommand("{ ping: 1 }")
                .map(document -> {
                    if (document.containsKey("ok") && document.get("ok").equals(1.0)) {
                        log.debug("MongoDB bağlantı kontrolü başarılı");
                        return true;
                    }
                    log.warn("MongoDB bağlantı kontrolü başarısız: {}", document);
                    return false;
                })
                .onErrorResume(e -> {
                    log.error("MongoDB bağlantı kontrolü sırasında hata: {}", e.getMessage());
                    return Mono.just(false);
                });
    }
}
