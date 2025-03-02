package com.craftpilot.apigateway.config;

import com.craftpilot.apigateway.security.FirebaseAuthenticationFilter;
import com.craftpilot.apigateway.security.SecurityConstants;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.http.HttpStatus;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    private final FirebaseAuthenticationFilter firebaseAuthenticationFilter;
    private final CorsWebFilter corsWebFilter;

    public SecurityConfig(FirebaseAuthenticationFilter firebaseAuthenticationFilter, CorsWebFilter corsWebFilter) {
        this.firebaseAuthenticationFilter = firebaseAuthenticationFilter;
        this.corsWebFilter = corsWebFilter;
    }

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http
            .cors(cors -> {}) // CORS yapılandırmasını etkinleştir
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .httpBasic(httpBasic -> httpBasic.disable()) // HTTP Basic'i tamamen devre dışı bırakalım
            .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
            .logout(ServerHttpSecurity.LogoutSpec::disable)
            .authorizeExchange(exchanges -> exchanges
                .pathMatchers(SecurityConstants.PUBLIC_PATHS.toArray(new String[0])).permitAll()
                .pathMatchers("/admin/**").hasRole("ADMIN")
                .anyExchange().authenticated()
            )
            .addFilterAt(corsWebFilter, SecurityWebFiltersOrder.CORS)
            .addFilterAt(firebaseAuthenticationFilter, SecurityWebFiltersOrder.AUTHENTICATION)
            .headers(headers -> headers
                .frameOptions(frameOptions -> frameOptions.disable()) 
                .contentSecurityPolicy(csp -> csp
                    .policyDirectives("default-src 'self'; " +
                                    "script-src 'self' 'unsafe-inline' 'unsafe-eval'; " +
                                    "style-src 'self' 'unsafe-inline'; " +
                                    "img-src 'self' data:; " +
                                    "font-src 'self' data:; " +
                                    "connect-src 'self' *")
                )
            )
            .exceptionHandling(handling -> handling
                .authenticationEntryPoint((exchange, ex) -> {
                    // Basic Authentication yerine sadece Bearer kullanıyoruz
                    exchange.getResponse().getHeaders().set("WWW-Authenticate", "Bearer realm=\"craftpilot\"");
                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                    
                    // CORS için ekstra başlıklar ekleyebiliriz
                    String origin = exchange.getRequest().getHeaders().getOrigin();
                    if (origin != null) {
                        exchange.getResponse().getHeaders().set("Access-Control-Allow-Origin", origin);
                        exchange.getResponse().getHeaders().set("Access-Control-Allow-Credentials", "true");
                    }
                    
                    return exchange.getResponse().setComplete();
                })
            )
            .build();
    }
}