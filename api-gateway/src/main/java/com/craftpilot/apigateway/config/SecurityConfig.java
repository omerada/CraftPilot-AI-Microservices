package com.craftpilot.apigateway.config;

import com.craftpilot.apigateway.security.FirebaseAuthenticationFilter;
import com.craftpilot.apigateway.security.SecurityConstants;
import com.craftpilot.apigateway.security.SecurityPathsMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.ServerSecurityContextRepository;
import org.springframework.security.web.server.context.WebSessionServerSecurityContextRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    private final FirebaseAuthenticationFilter firebaseAuthenticationFilter;
    private final ReactiveAuthenticationManager authManager;

    public SecurityConfig(
            FirebaseAuthenticationFilter firebaseAuthenticationFilter,
            ReactiveAuthenticationManager authManager) {
        this.firebaseAuthenticationFilter = firebaseAuthenticationFilter;
        this.authManager = authManager;
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
    public ServerSecurityContextRepository securityContextRepository() {
        return new WebSessionServerSecurityContextRepository();
    }

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        ServerSecurityContextRepository securityContextRepository = securityContextRepository();
        
        SecurityPathsMatcher securityPathsMatcher = new SecurityPathsMatcher();
        
        return http
            .cors(cors -> {})
            .csrf(csrf -> csrf.disable())
            .httpBasic(basic -> basic.disable())
            .formLogin(form -> form.disable())
            .logout(logout -> logout.disable())
            
            // AuthenticationManager ve SecurityContextRepository'yi ayarla
            .authenticationManager(authManager)
            .securityContextRepository(securityContextRepository)
            
            // Yetkilendirme kuralları - ÖNEMLİ DEĞİŞİKLİK - TÜM İSTEKLERE İZİN VERİLİYOR
            .authorizeExchange(exchanges -> exchanges
                .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .pathMatchers(securityPathsMatcher.getPublicPaths()).permitAll()
                .pathMatchers("/admin/**").hasRole("ADMIN")
                .anyExchange().permitAll()  // <-- BU DEĞİŞTİ - TEMPORARY FIX
            )
            
            // Gateway filtreleri - Sırayı doğru ayarla
            .addFilterBefore(corsWebFilter(), SecurityWebFiltersOrder.CORS)
            .addFilterAt(firebaseAuthenticationFilter, SecurityWebFiltersOrder.AUTHENTICATION)
            
            // HTTP Headers
            .headers(headers -> headers
                .frameOptions(frameOptions -> frameOptions.mode(org.springframework.security.web.server.header.XFrameOptionsServerHttpHeadersWriter.Mode.DENY))
                .contentSecurityPolicy(csp -> csp
                    .policyDirectives("default-src 'self'; script-src 'self' 'unsafe-inline' 'unsafe-eval'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; font-src 'self' data:; connect-src 'self' *")
                )
                .cache(cache -> cache.disable()) // Cache kontrolünü devre dışı bırak
            )
            
            // Yetkilendirme hata işleme
            .exceptionHandling(handling -> handling
                .authenticationEntryPoint((exchange, ex) -> {
                    ServerHttpResponse response = exchange.getResponse();
                    
                    String requestPath = exchange.getRequest().getPath().value();
                    log.error("Authentication failure for path: {} - Error: {}", requestPath, ex.getMessage());
                    
                    if (!response.isCommitted()) {
                        // CORS headers
                        String origin = exchange.getRequest().getHeaders().getOrigin();
                        if (origin != null) {
                            response.getHeaders().set("Access-Control-Allow-Origin", origin);
                            response.getHeaders().set("Access-Control-Allow-Credentials", "true");
                        }
                        
                        // WWW-Authenticate düzenlemesi
                        response.getHeaders().remove(HttpHeaders.WWW_AUTHENTICATE);
                        response.getHeaders().set(HttpHeaders.WWW_AUTHENTICATE, "Bearer realm=\"craftpilot\"");
                        response.setStatusCode(HttpStatus.UNAUTHORIZED);
                    }
                    
                    return response.setComplete();
                })
            )
            .build();
    }
}