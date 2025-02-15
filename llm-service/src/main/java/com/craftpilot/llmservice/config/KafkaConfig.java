package com.craftpilot.llmservice.config;

import com.craftpilot.llmservice.config.KafkaBaseConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;

@Configuration
@EnableKafka
public class KafkaConfig extends KafkaBaseConfig {

    @Bean
    public NewTopic aiEventsTopic() {
        return createTopic("ai-events");
    }

    @Bean
    public NewTopic llmCompletionsTopic() {
        return createTopic("llm-completions");
    }
}