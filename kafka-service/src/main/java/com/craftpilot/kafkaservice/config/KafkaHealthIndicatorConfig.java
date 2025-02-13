package com.craftpilot.kafkaservice.config;

import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaAdmin;

@Configuration
public class KafkaHealthIndicatorConfig {

    @Bean
    public HealthIndicator kafkabrokerHealthIndicator(KafkaAdmin kafkaAdmin) {
        return new KafkaBrokerHealthIndicator(kafkaAdmin);
    }
}

class KafkaBrokerHealthIndicator implements HealthIndicator {
    private final KafkaAdmin kafkaAdmin;

    public KafkaBrokerHealthIndicator(KafkaAdmin kafkaAdmin) {
        this.kafkaAdmin = kafkaAdmin;
    }

    @Override
    public org.springframework.boot.actuate.health.Health health() {
        try {
            kafkaAdmin.initialize();
            return org.springframework.boot.actuate.health.Health.up().build();
        } catch (Exception e) {
            return org.springframework.boot.actuate.health.Health.down()
                .withException(e)
                .build();
        }
    }
}
