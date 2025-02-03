package com.craftpilot.translationservice.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {
    public static final String TRANSLATION_EVENTS_TOPIC = "translation-events";
    public static final String TRANSLATION_HISTORY_EVENTS_TOPIC = "translation-history-events";

    @Bean
    public NewTopic translationEventsTopic() {
        return TopicBuilder.name(TRANSLATION_EVENTS_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic translationHistoryEventsTopic() {
        return TopicBuilder.name(TRANSLATION_HISTORY_EVENTS_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }
} 