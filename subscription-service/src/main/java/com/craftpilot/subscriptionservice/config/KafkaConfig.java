package com.craftpilot.subscriptionservice.config;

import com.craftpilot.subscriptionservice.config.KafkaBaseConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
@ConditionalOnProperty(name = "kafka.enabled", havingValue = "true", matchIfMissing = true)
public class KafkaConfig extends KafkaBaseConfig {

    @Value("${spring.kafka.bootstrap-servers:kafka:9092}")
    private String bootstrapServers;


    @Bean
    public NewTopic subscriptionEventsTopic() {
        try {
            log.info("Creating or validating Kafka topic: subscription-events, using servers: {}", bootstrapServers);
            return createTopic("subscription-events");
        } catch (Exception e) {
            log.warn("Failed to create/validate Kafka topic. Service will continue startup. Error: {}", e.getMessage());
            // Return a temporary topic definition without actually trying to create it
            return TopicBuilder.name("subscription-events")
                    .partitions(1)
                    .replicas(1)
                    .build();
        }
    }

    @Bean
    public NewTopic paymentEventsTopic() {
        try {
            log.info("Creating or validating Kafka topic: payment-events, using servers: {}", bootstrapServers);
            return createTopic("payment-events");
        } catch (Exception e) {
            log.warn("Failed to create/validate Kafka topic. Service will continue startup. Error: {}", e.getMessage());
            return TopicBuilder.name("payment-events")
                    .partitions(1)
                    .replicas(1)
                    .build();
        }
    }
}