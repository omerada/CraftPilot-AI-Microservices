package com.craftpilot.userservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import com.craftpilot.userservice.config.LightSecurityConfig;
import org.springframework.context.annotation.Import;

@Import(LightSecurityConfig.class)
@SpringBootApplication
@EnableDiscoveryClient
@EnableReactiveMongoRepositories(basePackages = "com.craftpilot.userservice.repository")
public class UserserviceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserserviceApplication.class, args);
    }

}
