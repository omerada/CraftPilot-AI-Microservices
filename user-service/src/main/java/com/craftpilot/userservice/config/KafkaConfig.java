package com.craftpilot.userservice.config;

import com.craftpilot.shared.kafka.KafkaBaseConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;

@Configuration
@EnableKafka
public class KafkaConfig extends KafkaBaseConfig {

    @Bean
    public NewTopic userEventsTopic() {
        return createTopic("user-events");
    }

    @Bean
    public NewTopic userActivityTopic() {
        return createTopic("user-activity");
    }
}
