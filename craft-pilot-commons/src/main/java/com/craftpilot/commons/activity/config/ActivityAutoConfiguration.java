package com.craftpilot.commons.activity.config;

import com.craftpilot.commons.activity.aspect.ActivityLogAspect;
import com.craftpilot.commons.activity.logger.ActivityLogger;
import com.craftpilot.commons.activity.producer.ActivityProducer;
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
@ConditionalOnClass(KafkaTemplate.class)
@ConditionalOnProperty(prefix = "craftpilot.activity", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ActivityAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ActivityProducer activityProducer(KafkaTemplate<String, Object> kafkaTemplate, ActivityConfiguration config) {
        return new ActivityProducer(kafkaTemplate, config);
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
