package com.craftpilot.imageservice.config;

import com.craftpilot.imageservice.config.KafkaBaseConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.TopicBuilder;
import lombok.extern.slf4j.Slf4j;

@Configuration
@EnableKafka
@Slf4j
@ConditionalOnProperty(name = "spring.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class KafkaConfig extends KafkaBaseConfig {

    @Value("${kafka.topics.image-events}")
    private String imageEventsTopic;

    @Bean
    public NewTopic imageEventsTopic() {
        try {
            log.info("Creating or validating Kafka topic: {}", imageEventsTopic);
            return createTopic(imageEventsTopic);
        } catch (Exception e) {
            log.warn("Failed to create/validate Kafka topic: {}. Service will continue startup. Error: {}", 
                    imageEventsTopic, e.getMessage());
            // Return a temporary topic definition - actual creation will fail but won't stop application startup
            return TopicBuilder.name(imageEventsTopic)
                    .partitions(1)
                    .replicas(1)
                    .build();
        }
    }
    
    // Kafka bağlantısı başarısız olduğunda uygulamanın çökmemesi için fallback mekanizması
    @Bean
    @Primary
    @ConditionalOnProperty(name = "spring.kafka.enabled", havingValue = "false")
    public org.springframework.kafka.core.KafkaTemplate<?, ?> noopKafkaTemplate() {
        log.info("Creating no-op KafkaTemplate since Kafka is disabled");
        return null;
    }
}
