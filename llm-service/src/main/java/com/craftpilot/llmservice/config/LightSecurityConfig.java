package com.craftpilot.llmservice.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.server.WebFilter;
import reactor.core.publisher.Mono;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ServerWebExchange;

@Slf4j
@Configuration
public class LightSecurityConfig {

    private static final List<String> REQUIRED_HEADERS = Arrays.asList(
        "X-User-Id",
        "X-User-Role",
        "X-User-Email"
    );

    private static final List<String> PUBLIC_PATHS = Arrays.asList(
        "/actuator/health",
        "/actuator/info"
    );

    @Bean
    public WebFilter headerValidationFilter() {
        return (exchange, chain) -> {
            String path = exchange.getRequest().getPath().value();
            
            // Public path'leri kontrol etme
            if (isPublicPath(path)) {
                return chain.filter(exchange);
            }

            // Header kontrolü
            for (String header : REQUIRED_HEADERS) {
                String value = exchange.getRequest().getHeaders().getFirst(header);
                if (value == null || value.trim().isEmpty()) {
                    log.warn("Eksik header: {} - Path: {}", header, path);
                    return handleUnauthorized(exchange);
                }
            }

            return chain.filter(exchange);
        };
    }

    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }

    private Mono<Void> handleUnauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().add("X-Error-Message", "Gerekli headerlar eksik");
        return exchange.getResponse().setComplete();
    }
}
