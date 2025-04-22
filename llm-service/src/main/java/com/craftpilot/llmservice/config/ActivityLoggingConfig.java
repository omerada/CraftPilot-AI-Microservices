package com.craftpilot.llmservice.config;

import com.craftpilot.commons.activity.aspect.ActivityLogAspect;
import com.craftpilot.commons.activity.config.ActivityConfiguration;
import com.craftpilot.commons.activity.logger.ActivityLogger;
import com.craftpilot.commons.activity.producer.ActivityProducer;
import com.craftpilot.commons.activity.producer.KafkaActivityProducer;
import com.craftpilot.commons.activity.producer.LoggingActivityProducer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.kafka.core.KafkaTemplate;

@Configuration
@EnableAspectJAutoProxy
public class ActivityLoggingConfig {

    @Value("${activity.service-name-prefix:LLM}")
    private String serviceNamePrefix;

    @Value("${activity.kafka-topic:user-activity}")
    private String kafkaTopic;

    @Bean
    public ActivityConfiguration activityConfiguration() {
        ActivityConfiguration config = new ActivityConfiguration();
        config.setServiceNamePrefix(serviceNamePrefix);
        config.setTopic(kafkaTopic);
        return config;
    }

    @Bean
    @ConditionalOnProperty(name = "spring.kafka.bootstrap-servers")
    public ActivityProducer kafkaActivityProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        return new KafkaActivityProducer(kafkaTemplate, kafkaTopic);
    }

    @Bean
    @ConditionalOnProperty(name = "spring.kafka.bootstrap-servers", matchIfMissing = true, havingValue = "false")
    public ActivityProducer loggingActivityProducer() {
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
