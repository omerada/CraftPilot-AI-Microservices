package com.craftpilot.llmservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@EnableDiscoveryClient
@ComponentScan(basePackages = {"com.craftpilot.llmservice"})
public class LLMServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(LLMServiceApplication.class, args);
    }
}
