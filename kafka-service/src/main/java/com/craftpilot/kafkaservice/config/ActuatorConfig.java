package com.craftpilot.kafkaservice.config;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ActuatorConfig {

    @Bean
    public HealthIndicator simpleHealthCheck() {
        return () -> Health.up()
                .withDetail("app", "Kafka Service")
                .withDetail("description", "Service is running")
                .build();
    }
}
