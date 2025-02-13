package com.craftpilot.kafka.config;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.KafkaTemplate;

@Configuration
public class HealthCheckConfig {

    @Bean(name = "kafkaHealth")
    public HealthIndicator kafkaHealthIndicator(
            KafkaTemplate<String, String> kafkaTemplate,
            KafkaListenerEndpointRegistry registry) {
        return () -> {
            try {
                if (kafkaTemplate == null) {
                    return Health.down()
                            .withDetail("error", "KafkaTemplate is not available")
                            .build();
                }

                // Check Kafka connection
                kafkaTemplate.getDefaultTopic();
                
                // Check if any listeners are registered and running
                boolean listenersOk = registry.getListenerContainers().isEmpty() || 
                    registry.getListenerContainers().stream()
                        .anyMatch(container -> container.isRunning());

                if (listenersOk) {
                    return Health.up()
                            .withDetail("status", "Kafka is operational")
                            .withDetail("listeners", "OK")
                            .build();
                } else {
                    return Health.down()
                            .withDetail("error", "Kafka listeners are not running")
                            .build();
                }
            } catch (Exception e) {
                return Health.down()
                        .withDetail("error", "Kafka connection failed")
                        .withDetail("message", e.getMessage())
                        .build();
            }
        };
    }
}
