package com.craftpilot.apigateway.config;

import com.craftpilot.apigateway.filter.FirebaseAuthFilter;
import com.google.firebase.auth.FirebaseAuth;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.HttpStatusServerEntryPoint;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import reactor.core.publisher.Mono;

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
            .csrf(csrf -> csrf.disable())  // Disable CSRF for API Gateway
            .cors(corsSpec -> corsSpec.configurationSource(corsConfigurationSource()))
            .httpBasic(basic -> basic.disable())  // Basic auth'u tamamen kapatıyoruz
            .formLogin(form -> form.disable())    // Form login'i tamamen kapatıyoruz
            .logout(logout -> logout.disable())   // Logout endpoint'i kapatıyoruz
            .anonymous(anonymous -> anonymous.disable())  // Anonim erişimi kapatıyoruz
            .headers(headers -> headers
                // Replace deprecated frameOptions with modern security headers
                .xssProtection(xss -> xss.disable())
                .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'; frame-ancestors 'self'")))
            .authorizeExchange(exchanges -> exchanges
                .pathMatchers(HttpMethod.OPTIONS).permitAll()
                .pathMatchers(PUBLIC_PATHS.toArray(new String[0])).permitAll()
                .anyExchange().permitAll())
            .addFilterAt(new FirebaseAuthFilter(firebaseAuth, userPreferenceCache), SecurityWebFiltersOrder.AUTHENTICATION)
            .exceptionHandling(exceptionHandling -> exceptionHandling
                .authenticationEntryPoint(new HttpStatusServerEntryPoint(HttpStatus.UNAUTHORIZED))
                .accessDeniedHandler((exchange, denied) -> {
                    exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                    return exchange.getResponse().writeWith(
                        Mono.just(exchange.getResponse().bufferFactory().wrap(
                            "{\"error\":\"Erişim engellendi\",\"status\":403}".getBytes()
                        ))
                    );
                }))
            .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
            .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        
        // Origin patterns yerine specific origins kullan
        config.setAllowedOrigins(Arrays.asList(
            "http://localhost:5173",
            "https://app.craftpilot.io"
        ));
        
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
            "X-User-Id",
            "X-User-Email",
            "X-User-Role"
        ));
        
        config.setExposedHeaders(Arrays.asList(
            "X-Total-Count",
            "X-Error-Message"
        ));
        
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
