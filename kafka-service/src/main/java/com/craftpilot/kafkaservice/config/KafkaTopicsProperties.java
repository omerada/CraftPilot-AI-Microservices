package com.craftpilot.kafkaservice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

@Data
@Component
@ConfigurationProperties(prefix = "kafka")
public class KafkaTopicsProperties {
    private String bootstrapServers;
    private Map<String, TopicProperties> topics;
}

@Data
class TopicProperties {
    private String name;
    private int partitions;
    private int replicas;
}
