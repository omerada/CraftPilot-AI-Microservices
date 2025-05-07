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
            "/info"
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
             
            if (isPublicPath(path)) { 
                return chain.filter(exchange);
            }   
            
            // X-User-Id header'ı kontrolü
            String userId = request.getHeaders().getFirst("X-User-Id");
            
            if (userId == null || userId.isEmpty()) { 
                // İsteğe devam edilmiyor, 401 Unauthorized yanıtı dönülüyor
                exchange.getResponse().setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }  
            
            return chain.filter(exchange);
        };
    }

    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }
}
