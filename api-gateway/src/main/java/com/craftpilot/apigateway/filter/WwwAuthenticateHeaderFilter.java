package com.craftpilot.apigateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class WwwAuthenticateHeaderFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            HttpHeaders headers = exchange.getResponse().getHeaders();
            
            if (headers.containsKey(HttpHeaders.WWW_AUTHENTICATE)) {
                // Basic authentication kısmını tamamen kaldır
                headers.set(HttpHeaders.WWW_AUTHENTICATE, "Bearer realm=\"craftpilot\"");
            }
        }));
    }

    @Override
    public int getOrder() {
        // Mümkün olduğunca geç çalışması için yüksek bir değer
        return Ordered.LOWEST_PRECEDENCE;
    }
}
