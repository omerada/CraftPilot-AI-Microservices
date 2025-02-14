package com.craftpilot.kafkaservice.config;

import org.springframework.boot.actuate.health.DefaultHealthContributorRegistry;
import org.springframework.boot.actuate.health.HealthContributorRegistry;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.HealthEndpointGroups;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashMap;
import java.time.Duration;

@Configuration
public class KafkaHealthConfig {
    
    @Bean
    public HealthContributorRegistry healthContributorRegistry() {
        return new DefaultHealthContributorRegistry(new LinkedHashMap<>());
    }

    @Bean
    public HealthEndpoint healthEndpoint(
            HealthContributorRegistry registry,
            HealthEndpointGroups groups) {
        return new HealthEndpoint(registry, groups, Duration.ofSeconds(10));
    }
}
