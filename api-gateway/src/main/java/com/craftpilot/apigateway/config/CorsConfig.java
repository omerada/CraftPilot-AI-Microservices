package com.craftpilot.apigateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfig {

    @Value("${cors.allowed-origins:https://craftpilot.app,https://app.craftpilot.io,https://api.craftpilot.io,http://localhost:5173,http://localhost:3000}")
    private List<String> allowedOrigins;

    @Value("${cors.allowed-methods:GET,POST,PUT,DELETE,OPTIONS,PATCH}")
    private List<String> allowedMethods;

    @Value("${cors.max-age:3600}")
    private Long maxAge;

    @Value("${cors.allow-credentials:true}")
    private Boolean allowCredentials;

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration corsConfig = new CorsConfiguration();
        
        // Origins
        corsConfig.setAllowedOrigins(allowedOrigins);
        
        // Methods
        corsConfig.setAllowedMethods(allowedMethods);
        
        // Headers
        corsConfig.setAllowedHeaders(Arrays.asList(
            "Origin",
            "Content-Type",
            "Accept",
            "Authorization",
            "X-Requested-With",
            "X-User-Id",
            "X-User-Role",
            "X-User-Email",
            "X-Api-Key",
            "X-Request-ID",
            "Cache-Control",
            "If-Match",
            "If-None-Match"
        ));
        
        // Exposed Headers
        corsConfig.setExposedHeaders(Arrays.asList(
            "X-Error-Message",
            "X-Rate-Limit-Remaining",
            "X-Total-Count",
            "X-Response-Time",
            "ETag"
        ));
        
        // Credentials
        corsConfig.setAllowCredentials(allowCredentials);
        
        // Max Age
        corsConfig.setMaxAge(maxAge);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);

        return new CorsWebFilter(source);
    }
}
