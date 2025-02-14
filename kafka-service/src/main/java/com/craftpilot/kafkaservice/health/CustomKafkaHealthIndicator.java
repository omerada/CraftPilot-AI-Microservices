package com.craftpilot.kafkaservice.health;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ListTopicsOptions;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class CustomKafkaHealthIndicator implements HealthIndicator {

    private final KafkaAdmin kafkaAdmin;

    public CustomKafkaHealthIndicator(KafkaAdmin kafkaAdmin) {
        this.kafkaAdmin = kafkaAdmin;
    }

    @Override
    public Health health() {
        try (AdminClient adminClient = AdminClient.create(kafkaAdmin.getConfigurationProperties())) {
            // Set a short timeout for the operation
            ListTopicsOptions options = new ListTopicsOptions();
            options.timeoutMs((int) TimeUnit.SECONDS.toMillis(5));
            
            // Try to list topics
            adminClient.listTopics(options).names().get(5, TimeUnit.SECONDS);
            
            return Health.up()
                    .withDetail("status", "Connected")
                    .build();
        } catch (Exception e) {
            return Health.down()
                    .withDetail("status", "Disconnected")
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
