package com.craftpilot.llmservice.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import reactor.core.publisher.Mono;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ServerWebExchange;
import java.util.List;
import java.util.Arrays;

@Slf4j
@Configuration
@EnableWebFluxSecurity
public class LightSecurityConfig {

    private static final List<String> PUBLIC_PATHS = Arrays.asList(
        "/actuator/",
        "/actuator/health",
        "/actuator/info"
    );

    private static final List<RequiredHeader> REQUIRED_HEADERS = Arrays.asList(
        new RequiredHeader("X-User-Id", "User ID is required"),
        new RequiredHeader("X-User-Role", "User role is required"),
        new RequiredHeader("X-User-Email", "User email is required")
    );

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.disable())
            .httpBasic(basic -> basic.disable())
            .formLogin(form -> form.disable())
            .authorizeExchange(exchanges -> exchanges
                .pathMatchers(PUBLIC_PATHS.toArray(new String[0])).permitAll()
                .anyExchange().authenticated()
            )
            .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
            .addFilter((exchange, chain) -> {
                String path = exchange.getRequest().getPath().value();
                
                if (isPublicPath(path)) {
                    return chain.filter(exchange);
                }

                for (RequiredHeader header : REQUIRED_HEADERS) {
                    String headerValue = exchange.getRequest().getHeaders().getFirst(header.name);
                    if (headerValue == null || headerValue.trim().isEmpty()) {
                        return handleMissingHeader(exchange, header.message);
                    }
                }

                return chain.filter(exchange);
            })
            .exceptionHandling(handling -> handling
                .authenticationEntryPoint((exchange, ex) -> {
                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                    return exchange.getResponse().setComplete();
                })
            )
            .build();
    }

    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
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
