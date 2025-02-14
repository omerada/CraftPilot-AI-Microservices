package com.craftpilot.kafkaservice.config;

import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.Status;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaHealthConfig {
    
    @Bean
    public HealthEndpoint healthEndpoint() {
        return new HealthEndpoint(healthContributorRegistry -> 
            healthContributorRegistry.getContributor("kafkaHealthIndicator")
                .map(contributor -> contributor.health())
                .orElse(Health.up().build()));
    }
}
