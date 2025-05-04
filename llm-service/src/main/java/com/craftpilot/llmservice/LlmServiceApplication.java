package com.craftpilot.llmservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;
import com.craftpilot.llmservice.config.LightSecurityConfig;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ComponentScan;

@Import(LightSecurityConfig.class) 
@SpringBootApplication
@EnableDiscoveryClient
@EnableScheduling 
@ComponentScan(basePackages = {
    "com.craftpilot.llmservice",
    "com.craftpilot.llmservice.config",
    "com.craftpilot.llmservice.controller",
    "com.craftpilot.llmservice.service",
    "com.craftpilot.llmservice.util"
})
public class LlmServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(LlmServiceApplication.class, args);
    }
}