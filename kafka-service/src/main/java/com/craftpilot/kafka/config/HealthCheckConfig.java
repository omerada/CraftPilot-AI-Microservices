package com.craftpilot.kafka.config;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.KafkaTemplate;

@Configuration
public class HealthCheckConfig {
    
    @Bean
    public HealthIndicator customKafkaHealthIndicator(
            KafkaTemplate<String, String> kafkaTemplate,
            KafkaListenerEndpointRegistry registry) {
        return () -> {
            try {
                kafkaTemplate.getDefaultTopic();
                boolean listenersRunning = registry.getListenerContainers().stream()
                    .allMatch(container -> container.isRunning());
                
                if (listenersRunning) {
                    return Health.up()
                        .withDetail("listeners", "running")
                        .build();
                } else {
                    return Health.down()
                        .withDetail("listeners", "not all running")
                        .build();
                }
            } catch (Exception e) {
                return Health.down()
                    .withException(e)
                    .build();
            }
        };
    }
}
