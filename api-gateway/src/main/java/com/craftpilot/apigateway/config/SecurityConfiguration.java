package com.craftpilot.apigateway.config;

import com.craftpilot.apigateway.security.FirebaseAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Mono;

@Configuration
@EnableWebFluxSecurity
@RequiredArgsConstructor
public class SecurityConfiguration {

    private final FirebaseAuthenticationFilter firebaseAuthenticationFilter;
    private final CorsWebFilter corsWebFilter;

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
            .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
            .logout(ServerHttpSecurity.LogoutSpec::disable)
            .authorizeExchange(exchanges -> exchanges
                .pathMatchers(HttpMethod.OPTIONS).permitAll()
                .pathMatchers(
                    "/actuator/**", 
                    "/v3/api-docs/**", 
                    "/swagger-ui/**",
                    "/webjars/**", 
                    "/fallback/**",
                    "/favicon.ico",
                    "/auth/login",
                    "/auth/register",
                    "/auth/reset-password").permitAll()
                .pathMatchers("/admin/**").hasRole("ADMIN")
                .anyExchange().authenticated()
            )
            .addFilterAt(corsWebFilter, SecurityWebFiltersOrder.CORS)
            .addFilterAt(firebaseAuthenticationFilter, SecurityWebFiltersOrder.AUTHENTICATION)
            .exceptionHandling(handling -> handling
                .authenticationEntryPoint((exchange, ex) -> {
                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                    return exchange.getResponse().writeWith(
                        Mono.just(exchange.getResponse().bufferFactory()
                            .wrap("Unauthorized access".getBytes()))
                    );
                })
                .accessDeniedHandler((exchange, denied) -> {
                    exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                    return exchange.getResponse().writeWith(
                        Mono.just(exchange.getResponse().bufferFactory()
                            .wrap("Access denied".getBytes()))
                    );
                })
            )
            .build();
    }
}
