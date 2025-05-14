package com.craftpilot.analyticsservice.config;

import com.craftpilot.analyticsservice.config.KafkaBaseConfig;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
@Slf4j
@ConditionalOnProperty(name = "kafka.enabled", havingValue = "true", matchIfMissing = false)
public class KafkaConfig extends KafkaBaseConfig {
    
    @Value("${kafka.topics.analytics-events}")
    private String analyticsEventsTopic;
    
    @Value("${kafka.topics.metrics-events}")
    private String metricsEventsTopic;
    
    @Value("${kafka.bootstrap-servers:${spring.kafka.bootstrap-servers:localhost:9092}}")
    private String bootstrapServers;
    
    @Bean
    public KafkaAdmin kafkaAdmin() {
        log.info("Kafka Admin yapılandırılıyor, broker adresi: {}", bootstrapServers);
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        // Admin istemcisi için timeout süresini artır
        configs.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, 10000);
        configs.put(AdminClientConfig.RETRIES_CONFIG, 3);
        configs.put(AdminClientConfig.RETRY_BACKOFF_MS_CONFIG, 1000);
        
        KafkaAdmin admin = new KafkaAdmin(configs);
        // Broker bulunamazsa uygulamanın çökmesini engelle
        admin.setFatalIfBrokerNotAvailable(false);
        admin.setAutoCreate(true);
        return admin;
    }

    @Bean
    public NewTopic analyticsEventsTopic() {
        log.info("Analytics events topic oluşturuluyor: {}", analyticsEventsTopic);
        return createTopic(analyticsEventsTopic);
    }

    @Bean
    public NewTopic metricsEventsTopic() {
        log.info("Metrics events topic oluşturuluyor: {}", metricsEventsTopic);
        return createTopic(metricsEventsTopic);
    }
}
