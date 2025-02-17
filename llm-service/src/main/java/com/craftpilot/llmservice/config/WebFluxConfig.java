package com.craftpilot.llmservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.server.WebFilter;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.config.WebFluxConfigurer;

@Configuration
@EnableWebFlux
public class WebFluxConfig implements WebFluxConfigurer {
    
    @Bean
    public WebFilter responseHeaderFilter() {
        return (exchange, chain) -> {
            exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
            return chain.filter(exchange);
        };
    }
}
