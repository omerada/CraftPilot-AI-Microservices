package com.craftpilot.apigateway.config;

import com.craftpilot.apigateway.security.FirebaseAuthenticationFilter;
import com.craftpilot.apigateway.security.SecurityConstants;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    private final FirebaseAuthenticationFilter firebaseAuthenticationFilter;

    public SecurityConfig(FirebaseAuthenticationFilter firebaseAuthenticationFilter) {
        this.firebaseAuthenticationFilter = firebaseAuthenticationFilter;
    }

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration config = new CorsConfiguration();
        
        // İzin verilen originler
        config.setAllowedOriginPatterns(Arrays.asList(
            "http://localhost:*", 
            "https://*.craftpilot.io", 
            "https://craftpilot.io"
        ));
        
        // İzin verilen metodlar
        config.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH", "HEAD"
        ));
        
        // İzin verilen başlıklar
        config.setAllowedHeaders(Arrays.asList(
            "Origin", "Content-Type", "Accept", "Authorization", "X-Requested-With",
            "X-User-Id", "X-User-Role", "X-User-Email"
        ));
        
        // Expose edilen başlıklar
        config.setExposedHeaders(Arrays.asList(
            "Access-Control-Allow-Origin",
            "Access-Control-Allow-Credentials",
            "X-Total-Count"
        ));
        
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        
        return new CorsWebFilter(source);
    }

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http
                .cors(cors -> {}) // CorsWebFilter zaten ekledik
                .csrf(csrf -> csrf.disable())
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .logout(logout -> logout.disable())
                
                // Yetkilendirme kuralları - Daha fazla path'i public yap
                .authorizeExchange(exchanges -> exchanges
                    .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll() // OPTIONS isteklerini daima permit et
                    .pathMatchers(SecurityConstants.PUBLIC_PATHS.toArray(new String[0])).permitAll()
                    .pathMatchers("/admin/**").hasRole("ADMIN")
                    .anyExchange().authenticated()
                )
                
                // Gateway filtreleri - Sadece FirebaseAuthenticationFilter kullan
                .addFilterAt(corsWebFilter(), SecurityWebFiltersOrder.CORS)
                .addFilterAfter(firebaseAuthenticationFilter, SecurityWebFiltersOrder.AUTHENTICATION) 
                
                // HTTP Headers yapılandırması
                .headers(headers -> headers
                    .frameOptions(frameOptions -> frameOptions.mode(org.springframework.security.web.server.header.XFrameOptionsServerHttpHeadersWriter.Mode.DENY))
                    .contentSecurityPolicy(csp -> csp
                        .policyDirectives("default-src 'self'; script-src 'self' 'unsafe-inline' 'unsafe-eval'; " +
                                        "style-src 'self' 'unsafe-inline'; img-src 'self' data:; " +
                                        "font-src 'self' data:; connect-src 'self' *")
                    )
                )
                
                // Yetkilendirme hata işleme - SPA yönlendirmesi için HTML ile düzenlenebilir
                .exceptionHandling(handling -> handling
                    .authenticationEntryPoint((exchange, ex) -> {
                        ServerHttpResponse response = exchange.getResponse();
                        
                        // Önce CORS başlıklarını ayarla
                        String origin = exchange.getRequest().getHeaders().getOrigin();
                        if (origin != null) {
                            response.getHeaders().set("Access-Control-Allow-Origin", origin);
                            response.getHeaders().set("Access-Control-Allow-Credentials", "true");
                        }
                        
                        // Basic Auth pop-up'ını önlemek için WWW-Authenticate başlığı düzenlemesi
                        // Basic yerine sadece Bearer içeriyor
                        response.getHeaders().remove(HttpHeaders.WWW_AUTHENTICATE);
                        response.getHeaders().set(HttpHeaders.WWW_AUTHENTICATE, "Bearer realm=\"craftpilot\"");
                        response.setStatusCode(HttpStatus.UNAUTHORIZED);
                        
                        return response.setComplete();
                    })
                )
                .build();
    }
}