package com.craftpilot.userservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;
import com.craftpilot.userservice.config.LightSecurityConfig;
import org.springframework.context.annotation.Import;

@Import(LightSecurityConfig.class)
@SpringBootApplication
@EnableDiscoveryClient
@EnableScheduling  
public class UserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
