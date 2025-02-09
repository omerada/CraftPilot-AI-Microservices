package com.craftpilot.apigateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.http.HttpMethod;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    private static final String[] SWAGGER_WHITELIST = {
        "/v3/api-docs/**",
        "/swagger-ui/**",
        "/swagger-ui.html",
        "/webjars/**",
        "/swagger-resources/**",
        "/configuration/**"
    };

    private static final String[] ACTUATOR_WHITELIST = {
        "/actuator/**",
        "/health/**"
    };

    @Bean
    @Order(1)
    public SecurityWebFilterChain swaggerSecurityFilterChain(ServerHttpSecurity http) {
        return http
            .securityMatcher(exchange -> {
                String path = exchange.getRequest().getURI().getPath();
                for (String pattern : SWAGGER_WHITELIST) {
                    if (path.matches(pattern.replace("**", ".*"))) {
                        return true;
                    }
                }
                return false;
            })
            .csrf().disable()
            .cors().disable()
            .authorizeExchange()
            .anyExchange().permitAll()
            .and()
            .build();
    }

    @Bean
    @Order(2)
    public SecurityWebFilterChain actuatorSecurityFilterChain(ServerHttpSecurity http) {
        return http
            .securityMatcher(exchange -> {
                String path = exchange.getRequest().getURI().getPath();
                for (String pattern : ACTUATOR_WHITELIST) {
                    if (path.matches(pattern.replace("**", ".*"))) {
                        return true;
                    }
                }
                return false;
            })
            .csrf().disable()
            .authorizeExchange()
            .anyExchange().permitAll()
            .and()
            .build();
    }

    @Bean
    @Order(3)
    public SecurityWebFilterChain mainSecurityFilterChain(ServerHttpSecurity http) {
        return http
            .csrf().disable()
            .authorizeExchange(exchanges -> exchanges
                .pathMatchers(HttpMethod.OPTIONS).permitAll()
                .pathMatchers("/api/**").authenticated()
                .anyExchange().authenticated()
            )
            // Firebase auth configuration will be here
            .build();
    }
}