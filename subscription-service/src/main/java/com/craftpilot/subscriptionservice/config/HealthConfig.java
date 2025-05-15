package com.craftpilot.subscriptionservice.config;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaAdmin;
import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class HealthConfig {

    @Bean
    public HealthIndicator kafkaHealthIndicator(KafkaAdmin kafkaAdmin) {
        return () -> {
            try {
                // Check if Kafka bootstrap servers are configured
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
