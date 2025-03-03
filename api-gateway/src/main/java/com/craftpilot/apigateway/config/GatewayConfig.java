package com.craftpilot.apigateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

@Configuration
public class GatewayConfig {
    
    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> Mono.just(
            exchange.getRequest().getHeaders().getFirst("X-User-Id") != null ?
            exchange.getRequest().getHeaders().getFirst("X-User-Id") :
            exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
        );
    }
}
