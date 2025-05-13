package com.craftpilot.creditservice.config;

import com.craftpilot.creditservice.config.KafkaBaseConfig;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Configuration
@Slf4j
@ConditionalOnProperty(name = "kafka.enabled", havingValue = "true", matchIfMissing = true)
public class KafkaConfig extends KafkaBaseConfig {

    @Value("${kafka.bootstrap-servers:kafka:9092}")
    private String bootstrapServers;
    
    @Value("${kafka.admin.fail-fast:false}")
    private boolean failFast;
    
    @Value("${kafka.admin.auto-create:false}")
    private boolean autoCreate;

    @Bean
    public NewTopic creditEventsTopic() {
        return createTopic("credit-events");
    }

    @Bean
    public NewTopic paymentEventsTopic() {
        return createTopic("payment-events");
    }
    
    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        // AdminClient bağlantı zaman aşımını azalt
        configs.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, 5000);
        configs.put(AdminClientConfig.RETRIES_CONFIG, 2);
        configs.put(AdminClientConfig.RETRY_BACKOFF_MS_CONFIG, 1000);
        
        KafkaAdmin admin = new KafkaAdmin(configs);
        admin.setFatalIfBrokerNotAvailable(failFast);
        admin.setAutoCreate(autoCreate);
        
        log.info("Configuring KafkaAdmin with brokers: {}, failFast: {}, autoCreate: {}", 
                bootstrapServers, failFast, autoCreate);
                
        return admin;
    }
}