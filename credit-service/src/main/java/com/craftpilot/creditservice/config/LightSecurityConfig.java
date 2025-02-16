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
                .anyExchange().authenticated()
            )
            .addFilterAt(headerValidationFilter(), SecurityWebFiltersOrder.AUTHENTICATION)
            .build();
    }

    @Bean
    public WebFilter headerValidationFilter() {
        return (exchange, chain) -> {
            String userId = exchange.getRequest().getHeaders().getFirst(USER_ID_HEADER);
            String userRole = exchange.getRequest().getHeaders().getFirst(USER_ROLE_HEADER);
            String correlationId = exchange.getRequest().getHeaders().getFirst(CORRELATION_ID_HEADER);
            String apiKey = exchange.getRequest().getHeaders().getFirst(API_KEY_HEADER);

            // Actuator endpoints için header kontrolünü bypass et
            String path = exchange.getRequest().getPath().value();
            if (path.startsWith("/actuator/") || path.startsWith("/v3/api-docs") || path.startsWith("/swagger-ui")) {
                return chain.filter(exchange);
            }

            // Header validasyonları
            if (userId == null || userRole == null || correlationId == null || apiKey == null) {
                log.warn("Missing required headers. Path: {}, CorrelationId: {}", path, correlationId);
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }

            // Log security info
            log.debug("Security headers validated. UserId: {}, Role: {}, CorrelationId: {}, Path: {}", 
                     userId, userRole, correlationId, path);

            // Request'e custom attributes ekle
            exchange.getAttributes().put("userId", userId);
            exchange.getAttributes().put("userRole", userRole);
            exchange.getAttributes().put("correlationId", correlationId);

            return chain.filter(exchange);
        };
    }
}
