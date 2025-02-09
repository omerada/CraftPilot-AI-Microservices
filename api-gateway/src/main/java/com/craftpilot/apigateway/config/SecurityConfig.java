package com.craftpilot.apigateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.http.HttpMethod;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.disable())
            .authorizeExchange(exchanges -> exchanges
                .pathMatchers(
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/webjars/**",
                    "/swagger-resources/**",
                    "/actuator/**",
                    "/health/**"
                ).permitAll()
                .pathMatchers(HttpMethod.OPTIONS).permitAll()
                .pathMatchers("/api/**").authenticated()
                .anyExchange().authenticated()
            )
            .build();
    }
}