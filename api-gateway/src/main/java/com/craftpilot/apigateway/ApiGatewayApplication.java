package com.craftpilot.apigateway;

import com.craftpilot.apigateway.filter.RequestLoggingFilter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.web.server.WebFilter;

/**
 * The entry point for the API Gateway Spring Boot application.
 * This application is a Eureka client that registers itself with a Eureka server.
 * The application is configured with the {@link SpringBootApplication} annotation.
 */
@SpringBootApplication
@EnableDiscoveryClient
public class ApiGatewayApplication {

    /**
     * Main method to run the Spring Boot application.
     *
     * @param args Command-line arguments passed during the application startup.
     */
    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }

    @Bean
    public WebFilter requestLoggingFilter() {
        return new RequestLoggingFilter();
    }
}
