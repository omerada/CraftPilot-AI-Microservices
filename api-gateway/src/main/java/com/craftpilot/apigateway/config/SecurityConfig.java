package com.craftpilot.apigateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.util.matcher.NegatedServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http
            .securityMatcher(new NegatedServerWebExchangeMatcher(
                ServerWebExchangeMatchers.pathMatchers("/swagger-ui/**", "/v3/api-docs/**", "/actuator/**")
            ))
            .csrf().disable()
            .cors().disable()
            .httpBasic().disable()
            .formLogin().disable()
            .authorizeExchange(exchanges -> exchanges
                .pathMatchers(
                    "/",
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/swagger-resources/**",
                    "/v3/api-docs/**",
                    "/webjars/**",
                    "/actuator/**",
                    "/favicon.ico",
                    "/api/**"
                ).permitAll()
                .anyExchange().permitAll()
            )
            .build();
    }
}