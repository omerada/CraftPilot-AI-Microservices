package com.craftpilot.apigateway.config;

import com.craftpilot.apigateway.filter.FirebaseAuthFilter;
import com.google.firebase.auth.FirebaseAuth;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;  // Add this import
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Configuration
@EnableWebFluxSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final FirebaseAuth firebaseAuth;
    
    private static final List<String> ALLOWED_ORIGINS = Arrays.asList(
        "http://localhost:5173",
        "http://localhost:3000",
        "https://*.craftpilot.io",
        "https://craftpilot.io"
    );

    private static final List<String> ALLOWED_HEADERS = Arrays.asList(
        "Authorization",
        "Content-Type",
        "X-User-Id",
        "X-User-Email",
        "X-User-Role"
    );

    private static final List<String> PUBLIC_PATHS = Arrays.asList(
        "/actuator/**",
        "/api/public/**",
        "/api/auth/signup",
        "/api/auth/login"
    );

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        FirebaseAuthFilter firebaseFilter = new FirebaseAuthFilter(firebaseAuth);

        return http
            .securityMatcher(ServerWebExchangeMatchers.anyExchange())
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .authorizeExchange(exchanges -> exchanges
                .pathMatchers(PUBLIC_PATHS.toArray(new String[0])).permitAll()
                .pathMatchers(HttpMethod.OPTIONS).permitAll()
                .anyExchange().permitAll()
            )
            .addFilterBefore(firebaseFilter, SecurityWebFiltersOrder.AUTHENTICATION)
            .httpBasic(basic -> basic.disable())
            .formLogin(form -> form.disable())
            .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
            .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        
        config.setAllowedOriginPatterns(ALLOWED_ORIGINS);
        config.setAllowedMethods(Arrays.asList(
            HttpMethod.GET.name(),
            HttpMethod.POST.name(),
            HttpMethod.PUT.name(),
            HttpMethod.DELETE.name(),
            HttpMethod.OPTIONS.name()
        ));
        
        config.setAllowedHeaders(Arrays.asList(
            HttpHeaders.AUTHORIZATION,
            HttpHeaders.CONTENT_TYPE,
            HttpHeaders.ACCEPT,
            "X-User-Id",
            "X-User-Email",
            "X-User-Role"
        ));
        
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
