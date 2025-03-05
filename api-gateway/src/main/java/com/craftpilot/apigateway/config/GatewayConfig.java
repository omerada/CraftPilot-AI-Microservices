package com.craftpilot.apigateway.config;

import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.reactive.ServerHttpRequest;
import reactor.core.publisher.Mono;

@Slf4j
@Configuration
public class GatewayConfig {
    
    @Bean
    public GlobalFilter removeWwwAuthenticateHeaderFilter() {
        return (exchange, chain) -> {
            return chain.filter(exchange).then(Mono.fromRunnable(() -> {
                // 401 yanıtında ve tüm yanıtlarda WWW-Authenticate header'ını kaldır
                exchange.getResponse().getHeaders().remove(HttpHeaders.WWW_AUTHENTICATE);
                log.debug("WWW-Authenticate header kaldırıldı");
            }));
        };
    }
    
    @Bean
    public GlobalFilter modifyAuthorizationHeaderFilter() {
        return (exchange, chain) -> {
            // Firebase token yerine belki backend servisin beklediği formatta token
            String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
            
            if (authHeader != null && !authHeader.isEmpty() && userId != null) {
                log.debug("AuthorizationHeader ve UserId mevcut, isteği iletiyorum");
                
                // Firebase token'ı olduğu gibi bırakıyoruz
                ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                    .header("X-Firebase-Verified", "true")
                    .build();
                    
                return chain.filter(exchange.mutate().request(mutatedRequest).build());
            }
            
            return chain.filter(exchange);
        };
    }
}
