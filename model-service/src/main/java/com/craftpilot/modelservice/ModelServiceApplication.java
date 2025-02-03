package com.craftpilot.modelservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class ModelServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ModelServiceApplication.class, args);
    }
} 