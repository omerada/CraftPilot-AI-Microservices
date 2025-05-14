package com.craftpilot.analyticsservice;

import com.craftpilot.analyticsservice.config.LightSecurityConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient; 
import org.springframework.context.annotation.Import; 


@SpringBootApplication
@EnableDiscoveryClient 
@Import(LightSecurityConfig.class)
public class AnalyticsServiceApplication {

    static {
        // Kafka admin client bağlantı zaman aşımlarını azaltmak için
        System.setProperty("admin.metadata.max.age.ms", "3000");
        System.setProperty("spring.kafka.admin.timeout", "10000");
        System.setProperty("spring.kafka.admin.fail-fast", "false");
        System.setProperty("spring.kafka.admin.client.timeout", "5000");
        System.setProperty("spring.kafka.listener.missing-topics-fatal", "false");
    }

    public static void main(String[] args) {
        SpringApplication.run(AnalyticsServiceApplication.class, args);
    }
}