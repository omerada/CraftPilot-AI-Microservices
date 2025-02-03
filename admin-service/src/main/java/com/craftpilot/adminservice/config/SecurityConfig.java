package com.craftpilot.adminservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.http.HttpMethod;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        // Swagger UI ve API docs için public erişim
                        .pathMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", 
                                    "/webjars/**", "/swagger-resources/**").permitAll()
                        // Actuator endpoint'leri için public erişim
                        .pathMatchers("/actuator/**").permitAll()
                        // OPTIONS istekleri için public erişim
                        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // Health check için public erişim
                        .pathMatchers("/health", "/info").permitAll()
                        // Diğer tüm istekler için basic auth
                        .anyExchange().authenticated()
                )
                .httpBasic(httpBasic -> httpBasic
                        .disable()
                )
                .build();
    }
} 