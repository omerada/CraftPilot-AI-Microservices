package com.craftpilot.imageservice.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.config.TopicBuilder;

public abstract class KafkaBaseConfig {
    
    @Value("${kafka.topic.partitions:3}")
    private int partitions;
    
    @Value("${kafka.topic.replicas:1}")
    private int replicas;
    
    protected NewTopic createTopic(String name) {
        return TopicBuilder.name(name)
                          .partitions(partitions)
                          .replicas(replicas)
                          .configs(getTopicConfig())
                          .build();
    }
    
    private Map<String, String> getTopicConfig() {
        Map<String, String> configs = new HashMap<>();
        configs.put("cleanup.policy", "delete");
        configs.put("retention.ms", "604800000"); // 7 days
        return configs;
    }
}
