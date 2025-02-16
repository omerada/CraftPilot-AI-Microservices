package com.craftpilot.subscriptionservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;
import com.craftpilot.subscriptionservice.config.LightSecurityConfig;
import org.springframework.context.annotation.Import;

@Import(LightSecurityConfig.class)
@SpringBootApplication
@EnableDiscoveryClient
@EnableScheduling
public class SubscriptionServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(SubscriptionServiceApplication.class, args);
    }
} 