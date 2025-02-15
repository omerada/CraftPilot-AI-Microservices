package com.craftpilot.imageservice.config;

import com.craftpilot.imageservice.config.KafkaBaseConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig extends KafkaBaseConfig {

    @Bean
    public NewTopic imageEventsTopic() {
        return createTopic("image-events");
    }

    @Bean
    public NewTopic imageGenerationEventsTopic() {
        return createTopic("image-generation-events");
    }
}
