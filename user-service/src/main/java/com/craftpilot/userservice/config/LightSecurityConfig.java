package com.craftpilot.userservice.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.server.WebFilter;
import reactor.core.publisher.Mono;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;

@Slf4j
@Configuration
@EnableWebFluxSecurity
public class LightSecurityConfig {

    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String USER_ROLE_HEADER = "X-User-Role";
    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    private static final String API_KEY_HEADER = "X-Api-Key";
    
    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
            .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
            .authorizeExchange(exchanges -> exchanges
                .pathMatchers("/actuator/health").permitAll()
                .pathMatchers("/actuator/info").permitAll()
                .pathMatchers("/v3/api-docs/**").permitAll()
                .pathMatchers("/swagger-ui/**").permitAll()
                .pathMatchers("/webjars/**").permitAll()  // Swagger UI için gerekli
                .pathMatchers("/swagger-ui.html").permitAll() // Ana Swagger sayfası için
                .anyExchange().authenticated()
            )
            .addFilterAt(headerValidationFilter(), SecurityWebFiltersOrder.AUTHENTICATION)
            .build();
    }

    @Bean
    public WebFilter headerValidationFilter() {
        return (exchange, chain) -> {
            String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
            String userRole = exchange.getRequest().getHeaders().getFirst("X-User-Role");
            String userEmail = exchange.getRequest().getHeaders().getFirst("X-User-Email");

            // Public endpoints bypass
            String path = exchange.getRequest().getPath().value();
            if (path.startsWith("/actuator/") || 
                path.startsWith("/v3/api-docs") || 
                path.startsWith("/swagger-ui") ||
                path.startsWith("/webjars/")) {  // webjars path'i bypass
                return chain.filter(exchange);
            }

            // Firebase user bilgilerini kontrol et
            if (userId == null || userRole == null) {
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }

            // Admin endpoint'leri için rol kontrolü
            if (path.startsWith("/admin") && !userRole.contains("ADMIN")) {
                exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                return exchange.getResponse().setComplete();
            }

            return chain.filter(exchange);
        };
    }
}
