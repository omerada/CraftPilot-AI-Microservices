package com.craftpilot.creditservice.config;

import com.craftpilot.creditservice.config.KafkaBaseConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig extends KafkaBaseConfig {

    @Bean
    public NewTopic creditEventsTopic() {
        return createTopic("credit-events");
    }

    @Bean
    public NewTopic paymentEventsTopic() {
        return createTopic("payment-events");
    }
}