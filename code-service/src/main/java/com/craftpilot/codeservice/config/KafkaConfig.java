package com.craftpilot.codeservice.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {
    public static final String CODE_EVENTS_TOPIC = "code-events";
    public static final String CODE_HISTORY_EVENTS_TOPIC = "code-history-events";

    @Bean
    public NewTopic codeEventsTopic() {
        return TopicBuilder.name(CODE_EVENTS_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic codeHistoryEventsTopic() {
        return TopicBuilder.name(CODE_HISTORY_EVENTS_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }
} 