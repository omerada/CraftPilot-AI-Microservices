package com.craftpilot.analyticsservice.config;

import com.craftpilot.analyticsservice.config.KafkaBaseConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.TopicBuilder;

import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Configuration
@EnableKafka
@ConditionalOnProperty(name = "kafka.enabled", havingValue = "true", matchIfMissing = true)
@Slf4j
public class KafkaConfig extends KafkaBaseConfig {
    
    @Value("${kafka.topics.analytics-events}")
    private String analyticsEventsTopic;
    
    @Value("${kafka.topics.metrics-events}")
    private String metricsEventsTopic;

    @Bean
    public NewTopic analyticsEventsTopic() {
        log.info("Creating Kafka topic: {}", analyticsEventsTopic);
        return TopicBuilder.name(analyticsEventsTopic)
                          .partitions(getPartitions())
                          .replicas(getReplicas())
                          .configs(getTopicConfig())
                          .build();
    }

    @Bean
    public NewTopic metricsEventsTopic() {
        log.info("Creating Kafka topic: {}", metricsEventsTopic);
        return TopicBuilder.name(metricsEventsTopic)
                          .partitions(getPartitions())
                          .replicas(getReplicas())
                          .configs(getTopicConfig())
                          .build();
    }
}
