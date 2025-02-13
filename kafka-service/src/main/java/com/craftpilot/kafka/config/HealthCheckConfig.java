package com.craftpilot.kafka.config;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.core.KafkaTemplate;

@Configuration
public class HealthCheckConfig {
    
    @Bean
    @Primary
    public HealthIndicator kafkaHealthIndicator(KafkaTemplate<String, String> kafkaTemplate) {
        return () -> {
            try {
                // Check if KafkaTemplate is properly initialized
                if (kafkaTemplate == null || kafkaTemplate.getProducerFactory() == null) {
                    return Health.down()
                        .withDetail("error", "Kafka configuration is missing")
                        .build();
                }
                
                return Health.up()
                        .withDetail("status", "Kafka is operational")
                        .build();
            } catch (Exception e) {
                return Health.down()
                        .withDetail("error", e.getMessage())
                        .build();
            }
        };
    }
}
