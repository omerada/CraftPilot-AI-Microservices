package com.craftpilot.activitylogservice.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.ReactiveHealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Configuration
@Slf4j
public class HealthConfig {

    @Bean
    public ReactiveHealthIndicator mongoHealthIndicator(ReactiveMongoTemplate mongoTemplate) {
        return () -> {
            log.debug("Checking MongoDB health");
            return mongoTemplate.executeCommand("{ ping: 1 }")
                    .timeout(Duration.ofSeconds(5))
                    .map(document -> {
                        log.debug("MongoDB health check successful");
                        return Health.up()
                                .withDetail("ping", "successful")
                                .build();
                    })
                    .onErrorResume(e -> {
                        log.warn("MongoDB health check failed: {}", e.getMessage());
                        return Mono.just(Health.down()
                                .withException(e)
                                .build());
                    });
        };
    }

    /**
     * Fallback Redis health indicator if no real Redis connection is configured
     * This prevents errors when Redis is included in health groups but not actually configured
     */
    @Bean
    @ConditionalOnMissingBean(name = "redisHealthIndicator")
    public ReactiveHealthIndicator fallbackRedisHealthIndicator() {
        log.info("Creating fallback Redis health indicator - Redis is not configured");
        return () -> Mono.just(Health.unknown()
                .withDetail("message", "Redis is not configured for this instance")
                .build());
    }
}
