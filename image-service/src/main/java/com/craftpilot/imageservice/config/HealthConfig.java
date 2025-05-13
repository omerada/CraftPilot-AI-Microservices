package com.craftpilot.imageservice.config;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import org.springframework.kafka.core.KafkaAdmin;

@Configuration
@Slf4j
public class HealthConfig {

    @Bean
    public HealthIndicator mongoHealthIndicator(ReactiveMongoTemplate mongoTemplate) {
        return () -> {
            try {
                // Just ping the database to check connectivity
                mongoTemplate.executeCommand("{ ping: 1 }")
                    .block();
                return Health.up().withDetail("database", "MongoDB").build();
            } catch (Exception e) {
                log.warn("MongoDB health check failed: {}", e.getMessage());
                return Health.down()
                    .withDetail("database", "MongoDB")
                    .withDetail("error", e.getMessage())
                    .build();
            }
        };
    }

    @Bean
    public HealthIndicator customKafkaHealthIndicator(KafkaAdmin kafkaAdmin) {
        return () -> {
            try {
                // Check if bootstrap servers are configured
                Object bootstrapServers = kafkaAdmin.getConfigurationProperties()
                    .get("bootstrap.servers");
                
                if (bootstrapServers != null && !String.valueOf(bootstrapServers).isEmpty()) {
                    return Health.up()
                        .withDetail("bootstrapServers", bootstrapServers)
                        .build();
                } else {
                    return Health.down()
                        .withDetail("message", "Kafka bootstrap servers not configured")
                        .build();
                }
            } catch (Exception e) {
                log.warn("Kafka health check failed: {}", e.getMessage());
                return Health.down()
                    .withDetail("error", e.getMessage())
                    .build();
            }
        };
    }
}
