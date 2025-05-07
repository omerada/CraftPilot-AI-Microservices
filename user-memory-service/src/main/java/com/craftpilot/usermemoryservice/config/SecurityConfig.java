package com.craftpilot.usermemoryservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.csrf.CookieServerCsrfTokenRepository;
import org.springframework.security.web.server.csrf.ServerCsrfTokenRequestAttributeHandler;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

@Configuration
@EnableWebFluxSecurity
@Slf4j
public class SecurityConfig {

    private static final List<String> PUBLIC_PATHS = Arrays.asList(
            "/actuator/",
            "/actuator/health",
            "/v3/api-docs",
            "/swagger-ui",
            "/webjars/",
            "/health",
            "/info"
    );

    // Microservice internal API paths that should be exempt from CSRF
    private static final List<String> INTERNAL_API_PATHS = Arrays.asList(
            "/memories/entries",
            "/user-instructions",
            "/user-context",
            "/user-preferences"
    );

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieServerCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new ServerCsrfTokenRequestAttributeHandler())
                        // CSRF korumasını internal API'lar için devre dışı bırak
                        .requireCsrfProtectionMatcher(exchange -> {
                            String path = exchange.getRequest().getURI().getPath();
                            
                            // Public paths veya internal API paths ise CSRF kontrolü yapma
                            if (isPublicPath(path) || isInternalApiPath(path)) {
                                return Mono.just(false);
                            }
                            
                            // Diğer tüm istekler için CSRF kontrolü yap
                            return Mono.just(true);
                        })
                )
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers("/**").permitAll()
                )
                .build();
    }

    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }
    
    private boolean isInternalApiPath(String path) {
        return INTERNAL_API_PATHS.stream().anyMatch(path::startsWith);
    }
    
    @Bean
    public WebFilter serviceAuthenticationFilter() {
        return (exchange, chain) -> {
            String path = exchange.getRequest().getURI().getPath();
            
            // Public paths için kontrol yapma
            if (isPublicPath(path)) {
                return chain.filter(exchange);
            }
            
            // Internal API paths için X-User-Id header kontrolü
            if (isInternalApiPath(path)) {
                String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
                if (userId == null || userId.isEmpty()) {
                    log.warn("Missing X-User-Id header for internal API request: {}", path);
                    exchange.getResponse().setStatusCode(HttpStatus.BAD_REQUEST);
                    return exchange.getResponse().writeWith(
                            Mono.just(exchange.getResponse()
                                .bufferFactory()
                                .wrap("{\"error\":\"X-User-Id header is required\"}".getBytes())));
                }
            }
            
            return chain.filter(exchange);
        };
    }
}
