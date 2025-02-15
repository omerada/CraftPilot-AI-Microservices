package com.craftpilot.analyticsservice.config;

import com.craftpilot.shared.kafka.KafkaBaseConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.TopicBuilder;

import java.util.Map;

@Configuration
@EnableKafka
public class KafkaConfig extends KafkaBaseConfig {
    
    @Value("${kafka.topics.analytics-events}")
    private String analyticsEventsTopic;
    
    @Value("${kafka.topics.metrics-events}")
    private String metricsEventsTopic;

    @Bean
    public NewTopic analyticsEventsTopic() {
        return TopicBuilder.name(analyticsEventsTopic)
                          .partitions(3)
                          .replicas(1)
                          .build();
    }

    @Bean
    public NewTopic metricsEventsTopic() {
        return TopicBuilder.name(metricsEventsTopic)
                          .partitions(3)
                          .replicas(1)
                          .build();
    }
}
