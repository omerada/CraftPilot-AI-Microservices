package com.craftpilot.adminservice.config;

import com.craftpilot.adminservice.config.KafkaBaseConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.config.TopicBuilder;
import javax.annotation.PostConstruct;

@Configuration
@EnableKafka
@Slf4j
@ConditionalOnProperty(name = "kafka.enabled", havingValue = "true", matchIfMissing = true)
public class KafkaConfig extends KafkaBaseConfig {

    @Value("${kafka.topics.admin-events:admin-events}")
    private String adminEventsTopic;

    @Value("${kafka.topics.system-metrics:system-metrics}")
    private String systemMetricsTopic;

    @Value("${kafka.topics.system-alerts:system-alerts}")
    private String systemAlertsTopic;

    @Value("${kafka.topics.audit-logs:audit-logs}")
    private String auditLogsTopic;

    @Value("${kafka.topics.activity-events:activity-events}")
    private String userActivityTopic;

    @PostConstruct
    public void init() {
        log.info(
                "Kafka configuration initialized with topics: admin-events={}, system-metrics={}, system-alerts={}, audit-logs={}, activity-events={}",
                adminEventsTopic, systemMetricsTopic, systemAlertsTopic, auditLogsTopic, userActivityTopic);
    }

    @Bean
    public NewTopic adminEventsTopic() {
        log.info("Creating Kafka topic: {}", adminEventsTopic);
        return TopicBuilder.name(adminEventsTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic systemMetricsTopic() {
        log.info("Creating Kafka topic: {}", systemMetricsTopic);
        return TopicBuilder.name(systemMetricsTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic systemAlertsTopic() {
        log.info("Creating Kafka topic: {}", systemAlertsTopic);
        return TopicBuilder.name(systemAlertsTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic auditLogsTopic() {
        log.info("Creating Kafka topic: {}", auditLogsTopic);
        return TopicBuilder.name(auditLogsTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic userActivityTopic() {
        log.info("Creating Kafka topic: {}", userActivityTopic);
        return TopicBuilder.name(userActivityTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
