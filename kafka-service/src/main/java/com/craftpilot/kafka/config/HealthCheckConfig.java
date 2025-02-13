package com.craftpilot.kafka.config;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.KafkaTemplate;

@Configuration
public class HealthCheckConfig {
    
    @Bean(name = "customKafka")  // Explicitly name the bean
    public HealthIndicator customKafkaHealthIndicator(
            KafkaTemplate<String, String> kafkaTemplate,
            KafkaListenerEndpointRegistry registry) {
        return () -> {
            try {
                // Basic connection check
                kafkaTemplate.getDefaultTopic();
                return Health.up()
                    .withDetail("message", "Kafka connection is available")
                    .build();
            } catch (Exception e) {
                return Health.down()
                    .withDetail("message", "Kafka connection is unavailable")
                    .withDetail("error", e.getMessage())
                    .build();
            }
        };
    }
}
