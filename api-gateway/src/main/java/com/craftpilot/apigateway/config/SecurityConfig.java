package com.craftpilot.apigateway.config;

import com.craftpilot.apigateway.security.FirebaseAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.ServerSecurityContextRepository;
import org.springframework.security.web.server.context.WebSessionServerSecurityContextRepository;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    private final FirebaseAuthenticationFilter firebaseAuthenticationFilter;

    public SecurityConfig(FirebaseAuthenticationFilter firebaseAuthenticationFilter) {
        this.firebaseAuthenticationFilter = firebaseAuthenticationFilter;
    }

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable) // HTTP Basic'i devre dışı bırak
            .formLogin(ServerHttpSecurity.FormLoginSpec::disable) // Form login'i devre dışı bırak
            .authorizeExchange(exchanges -> exchanges
                .pathMatchers("/actuator/**").permitAll()
                .pathMatchers("/**/actuator/**").permitAll()
                .pathMatchers("/*/actuator/**").permitAll()
                .pathMatchers("/*/health").permitAll()
                .pathMatchers("/v3/api-docs/**").permitAll()
                .pathMatchers("/swagger-ui/**").permitAll()
                .pathMatchers("/webjars/**").permitAll()
                .pathMatchers("/*/v3/api-docs/**").permitAll()
                .pathMatchers("/*/swagger-ui/**").permitAll()
                .pathMatchers("/*/webjars/**").permitAll()
                .pathMatchers("/auth/**").permitAll() // Auth endpointlerini public yap
                .pathMatchers("/users/register").permitAll() // Kayıt endpointini public yap
                .pathMatchers("/users/login").permitAll() // Login endpointini public yap
                .anyExchange().authenticated()
            )
            .addFilterAt(firebaseAuthenticationFilter, SecurityWebFiltersOrder.AUTHENTICATION)
            .build();
    }

    @Bean
    public ServerSecurityContextRepository securityContextRepository() {
        return new WebSessionServerSecurityContextRepository();
    }
}