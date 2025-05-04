package com.craftpilot.llmservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {
    "com.craftpilot.llmservice",
    "com.craftpilot.llmservice.config",
    "com.craftpilot.llmservice.util",
    "com.craftpilot.llmservice.controller",
    "com.craftpilot.llmservice.service"
})
public class LlmServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(LlmServiceApplication.class, args);
    }
}