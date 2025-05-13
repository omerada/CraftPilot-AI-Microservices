package com.craftpilot.adminservice.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class KafkaBaseConfig {

    @Value("${spring.kafka.bootstrap-servers:kafka:9092}")
    private String bootstrapServers;

    @Value("${kafka.topic.num-partitions:3}")
    private int numPartitions;

    @Value("${kafka.topic.replication-factor:1}")
    private short replicationFactor;

    /**
     * Creates a Kafka topic with default settings
     * 
     * @param name the name of the topic to create
     * @return a new topic configuration
     */
    protected NewTopic createTopic(String name) {
        log.debug("Creating Kafka topic: {} with {} partitions and replication factor {}",
                name, numPartitions, replicationFactor);

        return TopicBuilder.name(name)
                .partitions(numPartitions)
                .replicas(replicationFactor)
                .build();
    }

    /**
     * Creates a Kafka topic with custom partitions and replication factor
     * 
     * @param name       the name of the topic to create
     * @param partitions number of partitions
     * @param replicas   replication factor
     * @return a new topic configuration
     */
    protected NewTopic createTopic(String name, int partitions, short replicas) {
        log.debug("Creating Kafka topic: {} with {} partitions and replication factor {}",
                name, partitions, replicas);

        return TopicBuilder.name(name)
                .partitions(partitions)
                .replicas(replicas)
                .build();
    }
}
