package com.craftpilot.kafka.config;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthContributor;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaHealthConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = "kafka")
    public HealthContributor kafkaHealthContributor() {
        return (HealthIndicator) () -> Health.up().build();
    }
}
