package com.craftpilot.kafkaservice.config;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaAdmin;

@Configuration
public class KafkaHealthIndicatorConfig {

    @Bean("kafka")  // Bean adını açıkça belirtiyoruz
    public HealthIndicator kafkaHealthIndicator(KafkaAdmin kafkaAdmin) {
        return new KafkaBrokerHealthIndicator(kafkaAdmin);
    }
}

class KafkaBrokerHealthIndicator implements HealthIndicator {
    private final KafkaAdmin kafkaAdmin;

    public KafkaBrokerHealthIndicator(KafkaAdmin kafkaAdmin) {
        this.kafkaAdmin = kafkaAdmin;
    }

    @Override
    public Health health() {
        try {
            kafkaAdmin.initialize();
            return Health.up()
                    .withDetail("kafka", "Kafka broker is accessible")
                    .build();
        } catch (Exception e) {
            return Health.down()
                    .withDetail("kafka", "Kafka broker is not accessible")
                    .withException(e)
                    .build();
        }
    }
}
