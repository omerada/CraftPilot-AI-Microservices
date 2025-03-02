package com.craftpilot.apigateway.config;

import com.craftpilot.apigateway.security.FirebaseAuthenticationFilter;
import com.craftpilot.apigateway.security.SecurityConstants;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.web.cors.reactive.CorsWebFilter;

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
            // CORS - deprecated metodları kaldıralım 
            .cors(cors -> {}) // Doğru şekilde sadece cors() kullan
            .csrf(csrf -> csrf.disable())
            .httpBasic(basic -> basic.disable())
            .formLogin(form -> form.disable())
            .logout(logout -> logout.disable())
            
            // Yetkilendirme kuralları
            .authorizeExchange(exchanges -> exchanges
                .pathMatchers(SecurityConstants.PUBLIC_PATHS.toArray(new String[0])).permitAll()
                .pathMatchers("/admin/**").hasRole("ADMIN")
                .anyExchange().permitAll()
            )
            
            // Gateway filtreleri
            .addFilterBefore(corsWebFilter, SecurityWebFiltersOrder.CORS)
            
            // HTTP Headers - deprecated metodları kaldıralım
            .headers(headers -> headers
                // XFrameOptions'ı düzeltelim - DISABLE yerine doğru bir enum değeri kullanalım
                // Tercih 1: DENY kullanarak tüm frame embeddingi engelle
                .frameOptions(frameOptions -> frameOptions.mode(org.springframework.security.web.server.header.XFrameOptionsServerHttpHeadersWriter.Mode.DENY))
                
                // Alternatif: frameOptions'ı devre dışı bırakmak istiyorsak tamamen kaldırma
                // .frameOptions(frameOptions -> {}) // Bu şekilde tamamen devre dışı bırak
                
                .contentSecurityPolicy(csp -> csp
                    .policyDirectives("default-src 'self'; script-src 'self' 'unsafe-inline' 'unsafe-eval'; " +
                                    "style-src 'self' 'unsafe-inline'; img-src 'self' data:; " +
                                    "font-src 'self' data:; connect-src 'self' *")
                )
            )
            
            // Yetkilendirme hata işleme
            .exceptionHandling(handling -> handling
                .authenticationEntryPoint((exchange, ex) -> {
                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                    
                    // CORS başlıkları
                    String origin = exchange.getRequest().getHeaders().getOrigin();
                    if (origin != null) {
                        exchange.getResponse().getHeaders().set("Access-Control-Allow-Origin", origin);
                        exchange.getResponse().getHeaders().set("Access-Control-Allow-Credentials", "true");
                    }
                    
                    // WWW-Authenticate başlığı - SADECE Bearer
                    exchange.getResponse().getHeaders().set("WWW-Authenticate", "Bearer realm=\"craftpilot\"");
                    return exchange.getResponse().setComplete();
                })
            )
            .build();
    }
}