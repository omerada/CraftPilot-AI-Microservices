package com.craftpilot.apigateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class GlobalResponseHeaderFilter implements GlobalFilter, Ordered {
    private static final Logger log = LoggerFactory.getLogger(GlobalResponseHeaderFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return chain.filter(exchange)
            .then(Mono.fromRunnable(() -> {
                HttpHeaders headers = exchange.getResponse().getHeaders();
                
                // WWW-Authenticate başlığı kontrolü ve manipülasyonu
                if (headers.containsKey(HttpHeaders.WWW_AUTHENTICATE)) {
                    String authHeader = headers.getFirst(HttpHeaders.WWW_AUTHENTICATE);
                    if (authHeader != null && authHeader.contains("Basic")) {
                        log.debug("Removing Basic auth from WWW-Authenticate header");
                        headers.set(HttpHeaders.WWW_AUTHENTICATE, "Bearer realm=\"craftpilot\"");
                    }
                }
                
                // CORS başlıklarını ayarla
                String origin = exchange.getRequest().getHeaders().getOrigin();
                if (origin != null) {
                    headers.setAccessControlAllowOrigin(origin);
                    headers.setAccessControlAllowCredentials(true);
                }
            }));
    }

    @Override
    public int getOrder() {
        // En son çalışması için
        return Ordered.LOWEST_PRECEDENCE;
    }
}
