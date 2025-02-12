package com.craftpilot.kafkaservice.config;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        return new KafkaAdmin(configs);
    }

    @Bean
    public NewTopic questionEventsTopic() {
        return TopicBuilder.name("question-events")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic metricsEventsTopic() {
        return TopicBuilder.name("metrics-events")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic notificationEventsTopic() {
        return TopicBuilder.name("notification-events")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic contentEventsTopic() {
        return TopicBuilder.name("content-events")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic modelEventsTopic() {
        return TopicBuilder.name("model-events")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic imageEventsTopic() {
        return TopicBuilder.name("image-events")
                .partitions(3)
                .replicas(1)
                .build();
    }
}