package com.craftpilot.llmservice.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.server.WebFilter;
import reactor.core.publisher.Mono;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ServerWebExchange;
import java.util.List;   
import java.util.Arrays;   
import org.springframework.http.HttpMethod;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import org.springframework.web.cors.reactive.CorsWebFilter;

@Slf4j
@Configuration
@EnableWebFluxSecurity
public class LightSecurityConfig {

    @Value("${spring.security.cors.allowed-origins}")
    private List<String> allowedOrigins;

    private static final List<String> PUBLIC_PATHS = Arrays.asList(
        "/actuator/",
        "/v3/api-docs",
        "/swagger-ui",
        "/webjars/"
    );

    private static final List<RequiredHeader> REQUIRED_HEADERS = Arrays.asList(
        new RequiredHeader("X-User-Id", "User ID is required"),
        new RequiredHeader("X-User-Role", "User role is required"),
        new RequiredHeader("X-User-Email", "User email is required")
    );

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeExchange(auth -> auth
                .pathMatchers("/actuator/**", "/ai/**", "/v3/api-docs/**", "/swagger-ui/**").permitAll()
                .anyExchange().authenticated());
        
        return http.build();
    }

    @Bean
    public CorsWebFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.addAllowedMethod("*");
        config.addAllowedHeader("*");
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsWebFilter(source);
    }

    @Bean
    public WebFilter headerValidationFilter() {
        return (exchange, chain) -> {
            if (isPublicPath(exchange.getRequest().getPath().value())) {
                return chain.filter(exchange);
            }

            log.debug("Validating headers for path: {}", exchange.getRequest().getPath());

            for (RequiredHeader header : REQUIRED_HEADERS) {
                String headerValue = exchange.getRequest().getHeaders().getFirst(header.name);
                if (headerValue == null || headerValue.trim().isEmpty()) {
                    log.warn("Missing required header: {}", header.name);
                    return handleMissingHeader(exchange, header.message);
                }
            }

            // Header doğrulaması başarılı olduğunda bir authentication objesi oluştur
            return chain.filter(exchange)
                .contextWrite(context -> {
                    ApiKeyAuthentication auth = new ApiKeyAuthentication(
                        exchange.getRequest().getHeaders().getFirst("X-User-Id"),
                        exchange.getRequest().getHeaders().getFirst("X-User-Role"),
                        exchange.getRequest().getHeaders().getFirst("X-User-Email")
                    );
                    return ReactiveSecurityContextHolder.withAuthentication(auth);
                });
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

    private static class ApiKeyAuthentication implements Authentication {
        private final String userId;
        private final String role;
        private final String email;
        private boolean authenticated = true;

        public ApiKeyAuthentication(String userId, String role, String email) {
            this.userId = userId;
            this.role = role;
            this.email = email;
        }

        @Override
        public Collection<? extends GrantedAuthority> getAuthorities() {
            return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role));
        }

        @Override
        public Object getCredentials() {
            return null;
        }

        @Override
        public Object getDetails() {
            return Collections.unmodifiableMap(Map.of(
                "userId", userId,
                "role", role,
                "email", email
            ));
        }

        @Override
        public Object getPrincipal() {
            return userId;
        }

        @Override
        public boolean isAuthenticated() {
            return authenticated;
        }

        @Override
        public void setAuthenticated(boolean authenticated) throws IllegalArgumentException {
            this.authenticated = authenticated;
        }

        @Override
        public String getName() {
            return userId;
        }
    }
}
