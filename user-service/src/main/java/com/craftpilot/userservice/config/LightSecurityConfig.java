package com.craftpilot.userservice.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import reactor.core.publisher.Mono;
import org.springframework.http.HttpStatus;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Configuration
@EnableWebFluxSecurity
public class LightSecurityConfig {

    private static final List<String> PUBLIC_PATHS = Arrays.asList(
            "/actuator/",
            "/v3/api-docs",
            "/swagger-ui",
            "/webjars/",
            "/health",
            "/info");

    private static final List<RequiredHeader> REQUIRED_HEADERS = Arrays.asList(
            new RequiredHeader("X-User-Id", "User ID is required"),
            new RequiredHeader("X-User-Role", "User role is required"));

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .cors(cors -> cors.disable())
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers(getPublicPaths()).permitAll()
                        .pathMatchers("/admin/**").hasRole("ADMIN")
                        .anyExchange().authenticated())
                .addFilterAt(headerValidationFilter(), SecurityWebFiltersOrder.AUTHENTICATION)
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint((exchange, ex) -> {
                            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                            return exchange.getResponse().setComplete();
                        }))
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'"))
                        .xssProtection(xss -> xss.disable()))
                .build();
    }

    @Bean
    public WebFilter headerValidationFilter() {
        return (exchange, chain) -> {
            if (isPublicPath(exchange.getRequest().getPath().value())) {
                return chain.filter(exchange);
            }

            for (RequiredHeader header : REQUIRED_HEADERS) {
                String headerValue = exchange.getRequest().getHeaders().getFirst(header.name);
                if (headerValue == null || headerValue.trim().isEmpty()) {
                    return handleMissingHeader(exchange, header.message);
                }
            }

            return chain.filter(exchange);
        };
    }

    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }

    private String[] getPublicPaths() {
        return PUBLIC_PATHS.stream()
                .map(path -> path + "**")
                .toArray(String[]::new);
    }

    private Mono<Void> handleMissingHeader(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.BAD_REQUEST);
        exchange.getResponse().getHeaders().add("X-Error-Message", message);
        return exchange.getResponse().setComplete();
    }

    private static class RequiredHeader {
        final String name;
        final String message;

        RequiredHeader(String name, String message) {
            this.name = name;
            this.message = message;
        }
    }
}
