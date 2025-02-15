package com.craftpilot.adminservice.config;
 
import com.craftpilot.adminservice.config.KafkaBaseConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;

@Configuration
@EnableKafka
public class KafkaConfig extends KafkaBaseConfig {
    
    @Value("${kafka.topics.admin-events}")
    private String adminEventsTopic;
    
    @Value("${kafka.topics.system-metrics}")
    private String systemMetricsTopic;
    
    @Value("${kafka.topics.system-alerts}")
    private String systemAlertsTopic;
    
    @Value("${kafka.topics.audit-logs}")
    private String auditLogsTopic;
    
    @Value("${kafka.topics.user-activity}")
    private String userActivityTopic;

    @Bean
    public NewTopic adminEventsTopic() {
        return createTopic(adminEventsTopic);
    }

    @Bean
    public NewTopic systemMetricsTopic() {
        return createTopic(systemMetricsTopic);
    }

    @Bean
    public NewTopic systemAlertsTopic() {
        return createTopic(systemAlertsTopic);
    }

    @Bean
    public NewTopic auditLogsTopic() {
        return createTopic(auditLogsTopic);
    }

    @Bean
    public NewTopic userActivityTopic() {
        return createTopic(userActivityTopic);
    }
}
