package com.craftpilot.llmservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.http.HttpStatus;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import java.util.List;
import java.util.Arrays;

@Configuration
@EnableWebFluxSecurity
@Slf4j
public class LightSecurityConfig {

    // Sadece header varlığını kontrol edelim, detaylı validasyon yapmayalım
    private static final List<String> REQUIRED_HEADERS = List.of(
            "X-User-Id",
            "X-User-Email"
    );

    private static final List<String> PUBLIC_PATHS = Arrays.asList(
            "/actuator/**",
            "/health/**", 
            "/info/**"
    );

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers(PUBLIC_PATHS.toArray(new String[0])).permitAll()
                        .anyExchange().permitAll() // API Gateway'de doğrulama yapıldığından permitAll kullanabiliriz
                )
                .addFilterBefore(headerCheckFilter(), SecurityWebFiltersOrder.AUTHENTICATION)
                .build();
    }

    /**
     * Basit bir header kontrol filtresi
     * API Gateway tarafından gönderilen headerların varlığını kontrol eder
     */
    @Bean
    public WebFilter headerCheckFilter() {
        return (exchange, chain) -> {
            String path = exchange.getRequest().getPath().value();
            
            // Public pathler için kontrolü atla
            if (isPublicPath(path)) {
                log.debug("Public path erişimi: {}", path);
                return chain.filter(exchange);
            }
            
            // X-Skip-Authentication header varsa kontrolü atla (test için)
            if (Boolean.parseBoolean(exchange.getRequest().getHeaders()
                    .getFirst("X-Skip-Authentication"))) {
                log.debug("X-Skip-Authentication header tespit edildi, doğrulama atlanıyor");
                return chain.filter(exchange);
            }
            
            // Gerekli headerları kontrol et
            for (String headerName : REQUIRED_HEADERS) {
                String headerValue = exchange.getRequest().getHeaders().getFirst(headerName);
                if (headerValue == null || headerValue.trim().isEmpty()) {
                    log.warn("{} header'ı eksik veya boş", headerName);
                    exchange.getResponse().setStatusCode(HttpStatus.BAD_REQUEST);
                    return exchange.getResponse().writeWith(Mono.just(
                        exchange.getResponse().bufferFactory().wrap(
                            String.format("Gerekli header eksik: %s", headerName).getBytes()
                        )
                    ));
                }
            }
            
            log.debug("Header kontrolü başarılı, kullanıcı: {}", 
                      exchange.getRequest().getHeaders().getFirst("X-User-Id"));
            return chain.filter(exchange);
        };
    }

    /**
     * Path'in public olup olmadığını kontrol et
     */
    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream()
                .anyMatch(pattern -> 
                    pattern.endsWith("/**") 
                        ? path.startsWith(pattern.substring(0, pattern.length() - 3))
                        : path.equals(pattern)
                );
    }
}
