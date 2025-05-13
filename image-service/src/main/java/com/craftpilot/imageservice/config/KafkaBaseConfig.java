package com.craftpilot.imageservice.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.config.TopicBuilder;
import lombok.extern.slf4j.Slf4j;
import java.util.Map;
import java.util.HashMap;

@Slf4j
public abstract class KafkaBaseConfig {
    
    @Value("${kafka.topic.partitions:3}")
    private int partitions;
    
    @Value("${kafka.topic.replicas:1}")
    private int replicas;
    
    @Value("${spring.kafka.bootstrap-servers:kafka:9092}")
    private String bootstrapServers;
    
    protected NewTopic createTopic(String name) {
        log.info("Creating Kafka topic configuration: {} with {} partitions and {} replicas", 
                name, partitions, replicas);
        log.info("Using Kafka bootstrap servers: {}", bootstrapServers);
                
        Map<String, String> configs = new HashMap<>();
        configs.put("cleanup.policy", "delete");
        configs.put("retention.ms", "604800000"); // 7 days
        
        return TopicBuilder.name(name)
                          .partitions(partitions)
                          .replicas(replicas)
                          .configs(configs)
                          .build();
    }
}
