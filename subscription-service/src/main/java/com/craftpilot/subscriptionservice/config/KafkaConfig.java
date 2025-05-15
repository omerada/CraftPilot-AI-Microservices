package com.craftpilot.subscriptionservice.config;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Configuration
@Slf4j
@ConditionalOnProperty(name = "kafka.enabled", havingValue = "true", matchIfMissing = true)
public class KafkaConfig extends KafkaBaseConfig {

    @Value("${spring.kafka.bootstrap-servers:kafka:9092}")
    private String bootstrapServers;
    
    @Value("${spring.kafka.admin.operation-timeout:30000}")
    private int operationTimeout;
    
    @Value("${spring.kafka.admin.close-timeout:10000}")
    private int closeTimeout;
    
    @Value("${spring.kafka.admin.fail-fast:false}")
    private boolean failFast;
    
    @Value("${spring.kafka.admin.auto-create:false}")
    private boolean autoCreate;

    @Bean
    public KafkaAdmin kafkaAdmin() {
        log.info("Configuring KafkaAdmin with brokers: {}", bootstrapServers);
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        // Increase timeout values to avoid startup failures
        configs.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, operationTimeout);
        configs.put(AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, operationTimeout);
        configs.put(AdminClientConfig.RETRIES_CONFIG, 3);
        configs.put(AdminClientConfig.RETRY_BACKOFF_MS_CONFIG, 1000);
        
        KafkaAdmin admin = new KafkaAdmin(configs);
        // Don't fail the application startup if Kafka is not available
        admin.setFatalIfBrokerNotAvailable(failFast);
        admin.setAutoCreate(autoCreate);
        admin.setCloseTimeout(closeTimeout);
        admin.setOperationTimeout(operationTimeout);
        
        return admin;
    }

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