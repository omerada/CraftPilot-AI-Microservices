package com.craftpilot.kafka.config;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;

@Configuration
public class KafkaHealthIndicatorConfig {

    @Bean
    public HealthIndicator kafkaHealthIndicator(KafkaTemplate<String, String> kafkaTemplate) {
        return () -> {
            try {
                kafkaTemplate.getDefaultTopic();
                return Health.up().build();
            } catch (Exception e) {
                return Health.down()
                    .withException(e)
                    .build();
            }
        };
    }
}
