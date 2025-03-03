package com.craftpilot.apigateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import java.util.Arrays;
import java.util.List;
import org.springframework.http.HttpMethod;

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
        
        config.setAllowedOriginPatterns(Arrays.asList(
            "http://localhost:*",
            "https://*.craftpilot.io",
            "https://craftpilot.io"
        ));
        
        config.setAllowedMethods(Arrays.asList(
            HttpMethod.GET.name(),
            HttpMethod.POST.name(),
            HttpMethod.PUT.name(),
            HttpMethod.DELETE.name(),
            HttpMethod.OPTIONS.name()
        ));
        
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
            "X-Error-Message",
            "Access-Control-Allow-Origin"
        ));
        
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsWebFilter(source);
    }
}
