package com.craftpilot.llmservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Mono;
import java.util.Map;

@Configuration
@EnableWebFluxSecurity
public class LightSecurityConfig {

    private static final Map<String, String> REQUIRED_HEADERS = Map.of(
            "X-User-Id", "User ID is required",
            "X-User-Role", "User role is required",
            "X-User-Email", "User email is required"
    );

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        .anyExchange().permitAll()
                )
                .addFilterAt(validateHeadersFilter(), SecurityWebFiltersOrder.AUTHENTICATION)
                .build();
    }

    private org.springframework.web.server.WebFilter validateHeadersFilter() {
        return (exchange, chain) -> validateHeaders(exchange, chain);
    }

    private Mono<Void> validateHeaders(ServerWebExchange exchange, org.springframework.web.server.WebFilterChain chain) {
        for (Map.Entry<String, String> header : REQUIRED_HEADERS.entrySet()) {
            String headerValue = exchange.getRequest().getHeaders().getFirst(header.getKey());
            if (headerValue == null || headerValue.trim().isEmpty()) {
                exchange.getResponse().setStatusCode(HttpStatus.BAD_REQUEST);
                return exchange.getResponse().writeWith(Mono.just(
                    exchange.getResponse().bufferFactory().wrap(
                        header.getValue().getBytes()
                    )
                ));
            }
        }
        return chain.filter(exchange);
    }
}
