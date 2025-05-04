package com.craftpilot.usermemoryservice.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.web.server.WebFilter;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Configuration
public class LightSecurityConfig {

    private static final List<String> PUBLIC_PATHS = Arrays.asList(
            "/actuator",
            "/actuator/health",
            "/actuator/info",
            "/health",
            "/info",
            "/v3/api-docs",
            "/swagger-ui",
            "/webjars/"
    );

    @Bean
    @Order(0) // En önce çalışacak
    public WebFilter loggingHeadersFilter() {
        return (exchange, chain) -> {
            String path = exchange.getRequest().getPath().value();
            String method = exchange.getRequest().getMethod().name();
            
            if (isPublicPath(path)) { 
                return chain.filter(exchange);
            }
            
            // X-User-Id header'ı kontrolü
            String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
            
            if (userId != null && !userId.isEmpty()) {
                log.debug("İstek kimliği: {} {} - userId: {}", method, path, userId);
            } else {
                log.warn("X-User-Id header eksik: {} {}", method, path);
            }
            
            return chain.filter(exchange);
        };
    }
    
    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }
}
