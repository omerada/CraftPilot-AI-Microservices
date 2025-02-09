package com.craftpilot.apigateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.server.util.matcher.NegatedServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.OrServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.PathPatternParserServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;
import reactor.core.publisher.Mono;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    private static final String[] SWAGGER_WHITELIST = {
        "/v3/api-docs/**",
        "/swagger-ui/**",
        "/swagger-ui.html",
        "/webjars/**",
        "/swagger-resources/**",
        "/configuration/**"
    };

    private static final String[] ACTUATOR_WHITELIST = {
        "/actuator/**",
        "/health/**"
    };

    private ServerWebExchangeMatcher createMatcher(String[] patterns) {
        List<ServerWebExchangeMatcher> matchers = Arrays.stream(patterns)
            .map(pattern -> new PathPatternParserServerWebExchangeMatcher(pattern))
            .collect(Collectors.toList());
        return new OrServerWebExchangeMatcher(matchers);
    }

    @Bean
    @Order(1)
    public SecurityWebFilterChain swaggerSecurityFilterChain(ServerHttpSecurity http) {
        ServerWebExchangeMatcher swaggerMatcher = createMatcher(SWAGGER_WHITELIST);
        
        return http
            .securityMatcher(swaggerMatcher)
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.disable())
            .authorizeExchange(exchanges -> exchanges
                .anyExchange().permitAll()
            )
            .build();
    }

    @Bean
    @Order(2)
    public SecurityWebFilterChain actuatorSecurityFilterChain(ServerHttpSecurity http) {
        ServerWebExchangeMatcher actuatorMatcher = createMatcher(ACTUATOR_WHITELIST);
        
        return http
            .securityMatcher(actuatorMatcher)
            .csrf(csrf -> csrf.disable())
            .authorizeExchange(exchanges -> exchanges
                .anyExchange().permitAll()
            )
            .build();
    }

    @Bean
    @Order(3)
    public SecurityWebFilterChain mainSecurityFilterChain(ServerHttpSecurity http) {
        return http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.disable())
            .authorizeExchange(exchanges -> exchanges
                .pathMatchers(HttpMethod.OPTIONS).permitAll()
                .pathMatchers("/api/**").authenticated()
                .anyExchange().authenticated()
            )
            .build();
    }
}