package com.craftpilot.commons.activity.config;

import com.craftpilot.commons.activity.aspect.ActivityLogAspect;
import com.craftpilot.commons.activity.logger.ActivityLogger;
import com.craftpilot.commons.activity.producer.ActivityProducer;
import com.craftpilot.commons.activity.producer.KafkaActivityProducer;
import com.craftpilot.commons.activity.producer.LoggingActivityProducer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.KafkaTemplate;

@AutoConfiguration
@EnableConfigurationProperties(ActivityConfiguration.class)
@ConditionalOnProperty(prefix = "craftpilot.activity", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ActivityAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(KafkaTemplate.class)
    public ActivityProducer kafkaActivityProducer(KafkaTemplate<String, Object> kafkaTemplate, ActivityConfiguration config) {
        return new KafkaActivityProducer(kafkaTemplate, config.getTopic());
    }
    
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "spring.kafka.bootstrap-servers", matchIfMissing = true, havingValue = "false")
    public ActivityProducer loggingActivityProducer() {
        return new LoggingActivityProducer();
    }
    
    @Bean
    @ConditionalOnMissingBean
    public ActivityLogger activityLogger(ActivityProducer activityProducer, ActivityConfiguration config) {
        return new ActivityLogger(activityProducer, config);
    }
    
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(ActivityLogger.class)
    public ActivityLogAspect activityLogAspect(ActivityLogger activityLogger) {
        return new ActivityLogAspect(activityLogger);
    }
}
