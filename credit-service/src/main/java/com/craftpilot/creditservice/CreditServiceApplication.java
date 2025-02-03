package com.craftpilot.creditservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class CreditServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(CreditServiceApplication.class, args);
    }
} 