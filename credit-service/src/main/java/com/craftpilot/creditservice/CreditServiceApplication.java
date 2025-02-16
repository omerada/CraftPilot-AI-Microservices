package com.craftpilot.creditservice;

import com.craftpilot.creditservice.config.LightSecurityConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import com.craftpilot.creditservice.config.LightSecurityConfig;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@EnableDiscoveryClient
@Import(LightSecurityConfig.class)
public class CreditServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(CreditServiceApplication.class, args);
    }
}