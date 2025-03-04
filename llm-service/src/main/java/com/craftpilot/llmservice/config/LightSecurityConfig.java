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
import java.util.List;
import java.util.Arrays;

@Configuration
@EnableWebFluxSecurity
public class LightSecurityConfig {

    private static final Map<String, String> REQUIRED_HEADERS = Map.of(
            "X-User-Id", "User ID is required",
            "X-User-Role", "User role is required",
            "X-User-Email", "User email is required"
    );

    private static final List<String> PUBLIC_PATHS = Arrays.asList(
            "/actuator/**",
            "/actuator/health/**",
            "/actuator/info/**"
    );

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers(PUBLIC_PATHS.toArray(new String[0])).permitAll()
                        .anyExchange().authenticated()
                )
                .addFilterBefore(validateHeadersFilter(), SecurityWebFiltersOrder.AUTHENTICATION)
                .build();
    }

    private org.springframework.web.server.WebFilter validateHeadersFilter() {
        return (exchange, chain) -> {
            String path = exchange.getRequest().getPath().value();
            if (isPublicPath(path)) {
                return chain.filter(exchange);
            }
            return validateHeaders(exchange, chain);
        };
    }

    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream()
                .anyMatch(pattern -> 
                    pattern.endsWith("/**") 
                        ? path.startsWith(pattern.substring(0, pattern.length() - 3))
                        : path.equals(pattern)
                );
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
