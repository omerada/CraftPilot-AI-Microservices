package com.craftpilot.analyticsservice.config;

import org.springframework.boot.actuate.autoconfigure.health.ConditionalOnEnabledHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class ServiceHealthConfig {

    private final ReactiveMongoTemplate mongoTemplate;
    private final AtomicBoolean mongoDbConnected = new AtomicBoolean(false);

    @Bean
    @ConditionalOnEnabledHealthIndicator("mongodb")
    public HealthIndicator mongoHealthIndicator() {
        return () -> {
            try {
                // MongoDB bağlantısını sınırlı bir süre içinde test et
                boolean isConnected = mongoTemplate.collectionExists("test_connection")
                        .timeout(Duration.ofSeconds(3))
                        .onErrorReturn(false)
                        .block();
                
                mongoDbConnected.set(isConnected);
                
                if (isConnected) {
                    return Health.up()
                            .withDetail("database", "MongoDB")
                            .withDetail("status", "Connected")
                            .build();
                } else {
                    return Health.down()
                            .withDetail("database", "MongoDB")
                            .withDetail("status", "Disconnected")
                            .withDetail("error", "Unable to connect to MongoDB")
                            .build();
                }
            } catch (Exception e) {
                mongoDbConnected.set(false);
                log.warn("MongoDB health check failed: {}", e.getMessage());
                return Health.down()
                        .withDetail("database", "MongoDB")
                        .withDetail("status", "Error")
                        .withDetail("error", e.getMessage())
                        .build();
            }
        };
    }
    
    @Bean
    public HealthIndicator applicationHealthIndicator() {
        return () -> {
            if (mongoDbConnected.get()) {
                return Health.up()
                        .withDetail("appStatus", "Healthy")
                        .withDetail("mongoDbConnected", true)
                        .build();
            } else {
                return Health.status("WARNING")
                        .withDetail("appStatus", "Partial")
                        .withDetail("mongoDbConnected", false)
                        .withDetail("warning", "MongoDB connection is not available")
                        .build();
            }
        };
    }
}
