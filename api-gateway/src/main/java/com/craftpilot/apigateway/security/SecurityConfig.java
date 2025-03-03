package com.craftpilot.apigateway.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.http.HttpStatus;

@Configuration
@EnableWebFluxSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final FirebaseAuthenticationFilter firebaseAuthenticationFilter;
    private final CorsWebFilter corsWebFilter;

    private static final String[] PUBLIC_PATHS = {
        "/actuator/**",
        "/v3/api-docs/**",
        "/swagger-ui/**",
        "/webjars/**",
        "/fallback/**",
        "/auth/login",
        "/auth/register",
        "/auth/reset-password"
    };

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
            .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
            .logout(ServerHttpSecurity.LogoutSpec::disable)
            .authorizeExchange(exchanges -> exchanges
                .pathMatchers(PUBLIC_PATHS).permitAll()
                .pathMatchers("/admin/**").hasRole("ADMIN")
                .anyExchange().authenticated() // Tüm diğer istekler için authentication zorunlu
            )
            .addFilterAt(corsWebFilter, SecurityWebFiltersOrder.CORS)
            .addFilterAt(firebaseAuthenticationFilter, SecurityWebFiltersOrder.AUTHENTICATION)
            .exceptionHandling(handling -> handling
                .authenticationEntryPoint((exchange, ex) -> {
                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                    return exchange.getResponse().setComplete();
                })
                .accessDeniedHandler((exchange, denied) -> {
                    exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                    return exchange.getResponse().setComplete();
                })
            )
            .build();
    }
}
