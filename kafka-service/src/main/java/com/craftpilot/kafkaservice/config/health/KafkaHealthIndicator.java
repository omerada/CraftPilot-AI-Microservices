package com.craftpilot.kafkaservice.config.health;

import org.apache.kafka.clients.admin.AdminClient;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.stereotype.Component;

@Component("kafkaHealthIndicator")
public class KafkaHealthIndicator implements HealthIndicator {
    private final KafkaAdmin kafkaAdmin;

    public KafkaHealthIndicator(KafkaAdmin kafkaAdmin) {
        this.kafkaAdmin = kafkaAdmin;
    }

    @Override
    public Health health() {
        Health.Builder healthBuilder = new Health.Builder();
        try (AdminClient adminClient = AdminClient.create(kafkaAdmin.getConfigurationProperties())) {
            adminClient.listTopics().names().get();
            return healthBuilder
                    .up()
                    .withDetail("status", "Kafka is operational")
                    .build();
        } catch (Exception e) {
            return healthBuilder
                    .down()
                    .withDetail("status", "Kafka is not available")
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
