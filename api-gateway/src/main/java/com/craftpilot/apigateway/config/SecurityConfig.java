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
            // CSRF ve diğer güvenlik ayarları
            .cors().and()
            .csrf(csrf -> csrf.disable())
            .httpBasic(basic -> basic.disable())
            .formLogin(form -> form.disable())
            .logout(logout -> logout.disable())
            
            // Yetkilendirme kuralları - Hepsini izin ver, gerçek kontrol Firebase filter'da yapılacak
            .authorizeExchange(exchanges -> exchanges
                .pathMatchers(SecurityConstants.PUBLIC_PATHS.toArray(new String[0])).permitAll()
                .pathMatchers("/admin/**").hasRole("ADMIN")
                .anyExchange().permitAll()  // Gerçek kontrolü firebaseAuthenticationFilter yapacak
            )
            
            // Gateway filtreleri ve Spring Security filtrelerini ayrı tutma
            // firebaseAuthenticationFilter'ı buraya eklemeyin, zaten WebFilter olarak otomatik eklenecek
            .addFilterBefore(corsWebFilter, SecurityWebFiltersOrder.CORS)
            
            // HTTP Headers
            .headers(headers -> headers
                .frameOptions().disable()
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