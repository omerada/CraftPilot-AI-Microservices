package com.craftpilot.lighthouseservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LighthouseServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(LighthouseServiceApplication.class, args);
    }
}
