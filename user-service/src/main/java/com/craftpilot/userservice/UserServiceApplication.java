package com.craftpilot.userservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.data.mongodb.config.EnableReactiveMongoAuditing;
import org.springframework.context.annotation.Import;
import com.craftpilot.userservice.config.LightSecurityConfig;

@Import(LightSecurityConfig.class)
@SpringBootApplication
@EnableDiscoveryClient
@EnableScheduling
@EnableReactiveMongoAuditing
public class UserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
