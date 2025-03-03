package com.craftpilot.apigateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfiguration {

    @Value("${cors.allowed-origins:http://localhost:3000,http://localhost:5173}")
    private List<String> allowedOrigins;

    @Value("${cors.allowed-methods:GET,POST,PUT,DELETE,OPTIONS,PATCH}")
    private List<String> allowedMethods;

    @Value("${cors.max-age:3600}")
    private Long maxAge;

    @Bean
    public CorsWebFilter corsWebFilter() {
        org.springframework.web.cors.CorsConfiguration config = new org.springframework.web.cors.CorsConfiguration();
        
        // Wildcard domain desteği için pattern matching özelliğini aktif et
        config.setAllowedOriginPatterns(Arrays.asList(
            "http://localhost:*",
            "https://*.craftpilot.io",
            "https://craftpilot.io"
        ));
        
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(allowedMethods);
        config.setAllowedHeaders(Arrays.asList(
            "Origin", 
            "Content-Type",
            "Accept",
            "Authorization",
            "X-Requested-With",
            "X-User-Id",
            "X-User-Role",
            "X-User-Email"
        ));
        config.setExposedHeaders(Arrays.asList(
            "X-Total-Count",
            "X-Response-Time",
            "X-Error-Message"
        ));
        config.setAllowCredentials(true);
        config.setMaxAge(maxAge);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsWebFilter(source);
    }
}
