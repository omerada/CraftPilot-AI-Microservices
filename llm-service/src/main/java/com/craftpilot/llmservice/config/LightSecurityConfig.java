package com.craftpilot.llmservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.http.server.reactive.ServerHttpRequest;
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
            "/chat", // Chat endpoint'ini doğrudan ekleyelim
            "/chat/completions", // API Gateway'den gelen yol
            "/chat/completions/stream" // Stream endpoint'i
    );

    @Bean
    @Order(1) // Öncelik verelim
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .anonymous(anonymous -> anonymous.authorities("ROLE_ANONYMOUS"))
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers("/**").permitAll() // Tüm isteklere izin ver, header kontrolünü WebFilter ile yap
                )
                .build();
    }

    @Bean
    @Order(0) // En önce çalışacak
    public WebFilter loggingHeadersFilter() {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String path = request.getPath().value();
            
            log.debug("İstek geldi: {} {}", request.getMethod(), path);
            
            if (isPublicPath(path)) {
                log.debug("Public path erişimi: {} - kontrolsüz geçiyor", path);
                return chain.filter(exchange);
            }
            
            // Debug için tüm headerları yazdıralım
            request.getHeaders().forEach((key, values) -> 
                log.debug("Header: {} = {}", key, values));
            
            // X-User-Id header'ı kontrolü
            String userId = request.getHeaders().getFirst("X-User-Id");
            String skipAuth = request.getHeaders().getFirst("X-Skip-Authentication");
            
            if (skipAuth != null && "true".equalsIgnoreCase(skipAuth)) {
                log.debug("X-Skip-Authentication true olduğu için kimlik doğrulama atlanıyor");
                return chain.filter(exchange);
            }
            
            if (userId == null || userId.isEmpty()) {
                log.warn("Gerekli X-User-Id header eksik, ancak isteğe devam ediliyor");
                // İsteği reddetmek yerine loga yazıp devam edelim
                // API Gateway zaten yetkilendirmeyi yapıyor
            } else {
                log.debug("İstek kimlik doğrulaması başarılı: {}", userId);
            }
            
            return chain.filter(exchange);
        };
    }

    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }
}
