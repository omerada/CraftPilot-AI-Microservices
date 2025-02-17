package com.craftpilot.creditservice.config;

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

@Slf4j
@Configuration
@EnableWebFluxSecurity
public class LightSecurityConfig {

    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String USER_ROLE_HEADER = "X-User-Role";
    private static final String USER_EMAIL_HEADER = "X-User-Email";

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http
            .csrf().disable()
            .formLogin().disable()
            .httpBasic().disable()
            .headers(headers -> headers
                .frameOptions().disable()
                .cache().disable())
            .exceptionHandling(handling -> handling
                .authenticationEntryPoint((exchange, ex) -> {
                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                    return exchange.getResponse().setComplete();
                }))
            .authorizeExchange(exchanges -> exchanges
                .pathMatchers("/actuator/health", 
                             "/actuator/info",
                             "/v3/api-docs/**",
                             "/swagger-ui/**",
                             "/webjars/**",
                             "/swagger-ui.html").permitAll()
                .anyExchange().authenticated()
            )
            .addFilterAt(headerValidationFilter(), SecurityWebFiltersOrder.AUTHENTICATION)
            .build();
    }

    @Bean
    public WebFilter headerValidationFilter() {
        return (exchange, chain) -> {
            String path = exchange.getRequest().getPath().value();
            
            // Public endpoints bypass
            if (isPublicPath(path)) {
                return chain.filter(exchange);
            }

            // Header kontrolü
            if (!hasValidHeaders(exchange)) {
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }

            // Admin yetki kontrolü
            if (isAdminPath(path) && !isAdminUser(exchange)) {
                exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                return exchange.getResponse().setComplete();
            }

            return chain.filter(exchange);
        };
    }

    private boolean isPublicPath(String path) {
        return path.startsWith("/actuator/") || 
               path.startsWith("/v3/api-docs") || 
               path.startsWith("/swagger-ui") ||
               path.startsWith("/webjars/");
    }

    private boolean hasValidHeaders(ServerWebExchange exchange) {
        String userId = exchange.getRequest().getHeaders().getFirst(USER_ID_HEADER);
        String userRole = exchange.getRequest().getHeaders().getFirst(USER_ROLE_HEADER);
        return userId != null && userRole != null;
    }

    private boolean isAdminPath(String path) {
        return path.startsWith("/admin");
    }

    private boolean isAdminUser(ServerWebExchange exchange) {
        String userRole = exchange.getRequest().getHeaders().getFirst(USER_ROLE_HEADER);
        return userRole != null && userRole.contains("ADMIN");
    }
}
