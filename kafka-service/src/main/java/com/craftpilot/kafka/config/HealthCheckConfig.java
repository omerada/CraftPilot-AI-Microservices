package com.craftpilot.kafka.config;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.boot.actuate.health.Status;

@Configuration
public class HealthCheckConfig {

    @Bean
    public HealthIndicator customKafkaHealthIndicator(
            KafkaTemplate<String, String> kafkaTemplate,
            KafkaListenerEndpointRegistry registry) {
        return () -> {
            try {
                if (kafkaTemplate == null || kafkaTemplate.getProducerFactory() == null) {
                    return Health.status(Status.DOWN)
                            .withDetail("error", "Kafka configuration is missing")
                            .build();
                }
                return Health.status(Status.UP)
                        .withDetail("status", "Kafka connection is available")
                        .build();
            } catch (Exception e) {
                return Health.status(Status.DOWN)
                        .withDetail("error", "Kafka connection failed")
                        .withDetail("message", e.getMessage())
                        .build();
            }
        };
    }
}
