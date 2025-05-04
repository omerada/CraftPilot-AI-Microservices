package com.craftpilot.usermemoryservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class UserMemoryServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(UserMemoryServiceApplication.class, args);
    }
}
