package com.craftpilot.kafkaservice.config;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.health.ConditionalOnEnabledHealthIndicator;
import org.springframework.boot.actuate.autoconfigure.health.HealthContributorAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
@Import(HealthContributorAutoConfiguration.class)
@ConditionalOnEnabledHealthIndicator("kafka")
@RequiredArgsConstructor
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    private final KafkaTopicsProperties topicsProperties;

    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configs.put(AdminClientConfig.RETRIES_CONFIG, 5);
        configs.put(AdminClientConfig.RETRY_BACKOFF_MS_CONFIG, 1000);
        configs.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, 30000);
        configs.put(AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, "30000");
        configs.put(ProducerConfig.RECONNECT_BACKOFF_MS_CONFIG, "1000");
        return new KafkaAdmin(configs);
    }

    @Bean
    public NewTopic userEventsTopic() {
        return createTopic(topicsProperties.getTopics().get("user-events"));
    }

    @Bean
    public NewTopic subscriptionEventsTopic() {
        return createTopic(topicsProperties.getTopics().get("subscription-events"));
    }

    @Bean
    public NewTopic notificationEventsTopic() {
        return createTopic(topicsProperties.getTopics().get("notification-events"));
    }

    @Bean
    public NewTopic creditEventsTopic() {
        return createTopic(topicsProperties.getTopics().get("credit-events"));
    }

    @Bean
    public NewTopic imageEventsTopic() {
        return createTopic(topicsProperties.getTopics().get("image-events"));
    }

    @Bean
    public NewTopic analyticsEventsTopic() {
        return createTopic(topicsProperties.getTopics().get("analytics-events"));
    }

    @Bean
    public NewTopic llmEventsTopic() {
        return createTopic(topicsProperties.getTopics().get("llm-events"));
    }

    @Bean
    public NewTopic paymentEventsTopic() {
        return createTopic(topicsProperties.getTopics().get("payment-events"));
    }

    private NewTopic createTopic(TopicProperties properties) {
        return TopicBuilder.name(properties.getName())
                .partitions(properties.getPartitions())
                .replicas(properties.getReplicas())
                .config("min.insync.replicas", "2")
                .config("cleanup.policy", "delete")
                .config("retention.ms", "604800000") // 7 days
                .config("max.message.bytes", "1048576") // 1MB
                .build();
    }
}