package com.craftpilot.userservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import lombok.extern.slf4j.Slf4j;
import java.util.List;
import java.util.Arrays;

@Configuration
@EnableWebFluxSecurity
@Slf4j
public class LightSecurityConfig {

    private static final List<String> PUBLIC_PATHS = Arrays.asList(
            "/actuator",
            "/actuator/health",
            "/actuator/info",
            "/health",
            "/info",
            "/users/sync"
    );

    @Bean
    @Order(1) 
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .anonymous(anonymous -> anonymous.authorities("ROLE_ANONYMOUS"))
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers("/**").permitAll() 
                )
                .build();
    }

    @Bean
    @Order(0)  
    public WebFilter loggingHeadersFilter() {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String path = request.getPath().value();
            String method = request.getMethod().name();
            String clientIp = getClientIp(exchange);
            
            if (isPublicPath(path)) { 
                return chain.filter(exchange);
            }
            
            // Redis bağlantı yolları için ek log
            if (path.contains("/redis-health") || path.contains("/preferences")) {
                log.debug("Redis bağlantılı istek: {} {} [IP: {}]", method, path, clientIp);
            }
            
            // X-User-Id header'ı kontrolü
            String userId = request.getHeaders().getFirst("X-User-Id");
            
            if (userId == null || userId.isEmpty()) {
                log.debug("Gerekli X-User-Id header eksik: {} {} [IP: {}]", method, path, clientIp);
                
                // Tercihler için güvenlik by-pass: Geliştirme ortamında
                if (path.contains("/preferences")) {
                    log.debug("Preferences isteği için güvenlik by-pass aktif: {}", path);
                    // Burada default bir user-id ekleyebiliriz (geliştirme ortamı için)
                    exchange = exchange.mutate()
                        .request(exchange.getRequest().mutate()
                        .header("X-User-Id", "system-default")
                        .build())
                        .build();
                }
            } else {
                log.debug("İstek kimliği: {} {} - userId: {} [IP: {}]", method, path, userId, clientIp);
            }
            
            return chain.filter(exchange);
        };
    }

    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }
    
    private String getClientIp(ServerWebExchange exchange) {
        String forwardedFor = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isEmpty()) {
            return forwardedFor.split(",")[0].trim();
        }
        return exchange.getRequest().getRemoteAddress() != null ? 
            exchange.getRequest().getRemoteAddress().getHostString() : "unknown";
    }
}
