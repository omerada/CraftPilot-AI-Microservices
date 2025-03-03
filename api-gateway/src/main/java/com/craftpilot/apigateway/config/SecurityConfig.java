package com.craftpilot.apigateway.config;

import com.craftpilot.apigateway.security.FirebaseAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import reactor.core.publisher.Mono;

@Configuration
@EnableWebFluxSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    
    private final FirebaseAuthenticationFilter firebaseAuthenticationFilter;

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .cors(cors -> cors.disable())  // CORS'u disable ediyoruz çünkü Ingress'te yönetiyoruz
            .httpBasic(httpBasic -> httpBasic.disable())
            .formLogin(formLogin -> formLogin.disable())
            .logout(logout -> logout.disable())
            .authorizeExchange(exchanges -> exchanges
                .pathMatchers(HttpMethod.OPTIONS).permitAll()
                .pathMatchers(
                    "/actuator/**",
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/webjars/**",
                    "/fallback/**",
                    "/favicon.ico",
                    "/auth/**").permitAll()
                .anyExchange().authenticated()
            )
            .addFilterAfter(firebaseAuthenticationFilter, SecurityWebFiltersOrder.CORS)
            .exceptionHandling(exceptionHandling -> exceptionHandling
                .authenticationEntryPoint((exchange, ex) -> {
                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                    return exchange.getResponse().writeWith(
                        Mono.just(exchange.getResponse().bufferFactory()
                            .wrap("Unauthorized".getBytes()))
                    );
                })
            )
            .build();
    }
}
