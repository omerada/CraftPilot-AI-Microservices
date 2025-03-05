package com.craftpilot.apigateway.config;

import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Configuration
public class GatewayConfig {
    
    @Bean
    public GlobalFilter removeWwwAuthenticateHeaderFilter() {
        return (exchange, chain) -> {
            return chain.filter(exchange).then(Mono.fromRunnable(() -> {
                if (exchange.getResponse().getStatusCode() == HttpStatus.UNAUTHORIZED) {
                    exchange.getResponse().getHeaders().remove(HttpHeaders.WWW_AUTHENTICATE);
                    log.debug("Removed WWW-Authenticate header from 401 response");
                }
            }));
        };
    }
    
    @Bean
    public GlobalFilter preserveAuthorizationFilter() {
        return (exchange, chain) -> {
            // Forward the Authorization header to backend services 
            String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (authHeader != null && !authHeader.isEmpty()) {
                exchange.getRequest().mutate()
                    .header(HttpHeaders.AUTHORIZATION, authHeader)
                    .build();
                log.debug("Forwarding Authorization header to backend service");
            }
            
            return chain.filter(exchange);
        };
    }
}
