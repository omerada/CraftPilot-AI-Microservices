package com.craftpilot.creditservice.config;

import com.craftpilot.creditservice.event.CreditEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.core.KafkaTemplate;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

@Configuration
@Slf4j
@EnableConfigurationProperties(KafkaProperties.class)
public class KafkaAutoConfig {

    @Value("${kafka.bootstrap-servers:kafka:9092}")
    private String bootstrapServers;
    
    @Value("${kafka.enabled:true}")
    private boolean kafkaEnabled;

    @Bean
    @ConditionalOnProperty(name = "kafka.enabled", havingValue = "true", matchIfMissing = true)
    public ProducerFactory<String, CreditEvent> creditEventProducerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        configProps.put(ProducerConfig.ACKS_CONFIG, "1");
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3);
        configProps.put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, 1000);
        configProps.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 5000);
        configProps.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 5000);
        
        log.info("Configuring Kafka Producer with bootstrap servers: {}", bootstrapServers);
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    @Primary
    @ConditionalOnProperty(name = "kafka.enabled", havingValue = "true", matchIfMissing = true)
    public KafkaTemplate<String, CreditEvent> creditEventKafkaTemplate() {
        try {
            return new KafkaTemplate<>(creditEventProducerFactory());
        } catch (Exception e) {
            log.error("Failed to create Kafka template: {}", e.getMessage());
            return null;
        }
    }
    
    @Bean
    @ConditionalOnProperty(name = "kafka.enabled", havingValue = "false")
    public KafkaTemplate<String, CreditEvent> noopKafkaTemplate() {
        log.info("Kafka is disabled. Using no-op KafkaTemplate implementation");
        // Boş bir KafkaTemplate uygulaması oluştur
        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(new HashMap<>()));
    }
}
