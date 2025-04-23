package com.craftpilot.llmservice.config;

import com.craftpilot.commons.activity.aspect.ActivityLogAspect;
import com.craftpilot.commons.activity.config.ActivityConfiguration;
import com.craftpilot.commons.activity.logger.ActivityLogger;
import com.craftpilot.commons.activity.producer.ActivityProducer;
import com.craftpilot.commons.activity.producer.KafkaActivityProducer;
import com.craftpilot.commons.activity.producer.LoggingActivityProducer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.core.KafkaTemplate;

@Configuration
@EnableAspectJAutoProxy
@Slf4j
public class ActivityLoggingConfig {

    @Value("${activity.service-name-prefix:LLM}")
    private String serviceNamePrefix;

    @Value("${activity.kafka-topic:user-activity}")
    private String kafkaTopic;
    
    @Value("${spring.kafka.bootstrap-servers:#{null}}")
    private String bootstrapServers;

    @Bean
    public ActivityConfiguration activityConfiguration() {
        ActivityConfiguration config = new ActivityConfiguration();
        config.setServiceNamePrefix(serviceNamePrefix);
        config.setTopic(kafkaTopic);
        return config;
    }

    @Bean
    @Primary
    @ConditionalOnProperty(name = "spring.kafka.bootstrap-servers")
    public ActivityProducer kafkaActivityProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        log.info("Configuring Kafka activity producer with topic: {}", kafkaTopic);
        return new KafkaActivityProducer(kafkaTemplate, kafkaTopic);
    }

    @Bean
    @ConditionalOnMissingBean(ActivityProducer.class)
    public ActivityProducer loggingActivityProducer() {
        log.warn("Kafka bootstrap servers not configured, falling back to logging activity producer. Kafka connection: {}", 
              bootstrapServers != null ? bootstrapServers : "not configured");
        return new LoggingActivityProducer();
    }

    @Bean
    public ActivityLogger activityLogger(ActivityProducer activityProducer, ActivityConfiguration activityConfiguration) {
        return new ActivityLogger(activityProducer, activityConfiguration);
    }

    @Bean
    public ActivityLogAspect activityLogAspect(ActivityLogger activityLogger) {
        return new ActivityLogAspect(activityLogger);
    }
}
