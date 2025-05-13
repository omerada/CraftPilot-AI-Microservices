package com.craftpilot.creditservice;

import com.craftpilot.creditservice.config.LightSecurityConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@EnableDiscoveryClient
@Import(LightSecurityConfig.class)
@ConfigurationPropertiesScan
public class CreditServiceApplication {
    
    static {
        // Uygulamanın başlangıçta Apache Kafka admin client beklemesini azaltmak için
        System.setProperty("admin.metadata.max.age.ms", "3000");
        System.setProperty("kafka.admin.client.timeout", "5000");
    }
    
    public static void main(String[] args) {
        SpringApplication.run(CreditServiceApplication.class, args);
    }
}