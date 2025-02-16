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
    public static void main(String[] args) {
        SpringApplication.run(AnalyticsServiceApplication.class, args);
    }
} 