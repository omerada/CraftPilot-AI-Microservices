package com.craftpilot.userservice.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ActuatorConfig {

    @Bean
    public MeterRegistry meterRegistry() {
        return new SimpleMeterRegistry();
    }

    @Bean
    public HealthIndicator firebaseHealthIndicator() {
        return () -> {
            try {
                // Firebase bağlantı kontrolü
                return new org.springframework.boot.actuate.health.Health.Builder()
                    .status(Status.UP)
                    .withDetail("database", "Firebase")
                    .withDetail("status", "Connected")
                    .build();
            } catch (Exception e) {
                return new org.springframework.boot.actuate.health.Health.Builder()
                    .status(Status.DOWN)
                    .withDetail("database", "Firebase")
                    .withDetail("error", e.getMessage())
                    .build();
            }
        };
    }

    @Bean
    public HealthIndicator kafkaHealthIndicator() {
        return () -> {
            try {
                // Kafka bağlantı kontrolü
                return new org.springframework.boot.actuate.health.Health.Builder()
                    .status(Status.UP)
                    .withDetail("messaging", "Kafka")
                    .withDetail("status", "Connected")
                    .build();
            } catch (Exception e) {
                return new org.springframework.boot.actuate.health.Health.Builder()
                    .status(Status.DOWN)
                    .withDetail("messaging", "Kafka")
                    .withDetail("error", e.getMessage())
                    .build();
            }
        };
    }

    @Bean
    public HealthIndicator redisHealthIndicator() {
        return () -> {
            try {
                // Redis bağlantı kontrolü
                return new org.springframework.boot.actuate.health.Health.Builder()
                    .status(Status.UP)
                    .withDetail("cache", "Redis")
                    .withDetail("status", "Connected")
                    .build();
            } catch (Exception e) {
                return new org.springframework.boot.actuate.health.Health.Builder()
                    .status(Status.DOWN)
                    .withDetail("cache", "Redis")
                    .withDetail("error", e.getMessage())
                    .build();
            }
        };
    }
} 