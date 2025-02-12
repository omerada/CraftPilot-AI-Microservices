package com.craftpilot.kafkaservice.health;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.stereotype.Component;

@Component
public class KafkaHealthIndicator implements HealthIndicator {

    private final KafkaAdmin kafkaAdmin;
    private final String bootstrapServers;

    public KafkaHealthIndicator(KafkaAdmin kafkaAdmin, 
                              @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers) {
        this.kafkaAdmin = kafkaAdmin;
        this.bootstrapServers = bootstrapServers;
    }

    @Override
    public Health health() {
        try {
            kafkaAdmin.initialize();
            return Health.up()
                    .withDetail("bootstrapServers", bootstrapServers)
                    .build();
        } catch (Exception e) {
            return Health.down()
                    .withException(e)
                    .withDetail("bootstrapServers", bootstrapServers)
                    .build();
        }
    }
}
