package com.craftpilot.kafkaservice.health;

import org.apache.kafka.clients.admin.AdminClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class KafkaHealthIndicator implements HealthIndicator {

    private final KafkaAdmin kafkaAdmin;

    @Autowired
    public KafkaHealthIndicator(KafkaAdmin kafkaAdmin) {
        this.kafkaAdmin = kafkaAdmin;
    }

    @Override
    public Health health() {
        try (AdminClient adminClient = AdminClient.create(kafkaAdmin.getConfigurationProperties())) {
            adminClient.listTopics().names().get(5, TimeUnit.SECONDS);
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
