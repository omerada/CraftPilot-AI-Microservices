package com.craftpilot.userservice.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.server.WebFilter;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Arrays;

@Slf4j
@Configuration
@EnableWebFluxSecurity
public class LightSecurityConfig {

    private static final List<String> PUBLIC_PATHS = Arrays.asList(
            "/actuator/",
            "/v3/api-docs",
            "/swagger-ui",
            "/webjars/"
    );

    private static final List<String> REQUIRED_HEADERS = Arrays.asList(
            "X-User-Id",
            "X-User-Role"
    );

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers(getPublicPaths()).permitAll()
                        .anyExchange().authenticated()
                )
                .addFilterAt(headerValidationFilter(), org.springframework.security.config.web.server.SecurityWebFiltersOrder.AUTHENTICATION)
                .build();
    }

    @Bean
    public WebFilter headerValidationFilter() {
        return (exchange, chain) -> {
            String path = exchange.getRequest().getPath().value();
            
            if (isPublicPath(path)) {
                return chain.filter(exchange);
            }
            
            // API Gateway'den gelen header'ları kontrol et
            for (String headerName : REQUIRED_HEADERS) {
                String headerValue = exchange.getRequest().getHeaders().getFirst(headerName);
                if (headerValue == null || headerValue.isEmpty()) {
                    log.error("Required header missing: {}", headerName);
                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                    return exchange.getResponse().setComplete();
                }
            }
            
            // X-Auth-Processed header'ı API Gateway tarafından eklenmiş olmalı
            if (!"true".equals(exchange.getRequest().getHeaders().getFirst("X-Auth-Processed"))) {
                log.error("Auth not processed by API Gateway");
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }
            
            return chain.filter(exchange);
        };
    }

    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }

    private String[] getPublicPaths() {
        return PUBLIC_PATHS.stream()
                .map(path -> path + "**")
                .toArray(String[]::new);
    }
}
