package com.craftpilot.imageservice.config;

import com.craftpilot.imageservice.config.KafkaBaseConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig extends KafkaBaseConfig {

    @Value("${kafka.topics.image-events}")
    private String imageEventsTopic;

    @Bean
    public NewTopic imageEventsTopic() {
        return createTopic(imageEventsTopic);
    }
}
