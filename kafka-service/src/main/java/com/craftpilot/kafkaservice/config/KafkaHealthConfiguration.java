package com.craftpilot.kafkaservice.config;

import org.apache.kafka.clients.admin.AdminClient;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaAdmin;

@Configuration
public class KafkaHealthConfiguration {

    @Bean
    public HealthIndicator kafkaHealthIndicator(KafkaAdmin kafkaAdmin) {
        return () -> {
            try (AdminClient adminClient = AdminClient.create(kafkaAdmin.getConfigurationProperties())) {
                adminClient.listTopics().names().get();
                return Health.up().build();
            } catch (Exception e) {
                return Health.down()
                        .withException(e)
                        .build();
            }
        };
    }
}
