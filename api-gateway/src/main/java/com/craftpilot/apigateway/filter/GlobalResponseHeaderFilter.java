package com.craftpilot.apigateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Bu filtre, yanıt başlıklarını güvenli bir şekilde değiştirmek için kullanılır.
 * Başlık değişiklikleri, response commit edilmeden önce yapılmalıdır.
 */
@Component
public class GlobalResponseHeaderFilter implements GlobalFilter, Ordered {
    private static final Logger log = LoggerFactory.getLogger(GlobalResponseHeaderFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpResponse response = exchange.getResponse();
        
        // Yanıt başlıklarını güncellemeye çalışmadan önce zinciri devam ettirelim
        return chain.filter(exchange)
            .then(Mono.defer(() -> {
                try {
                    // WWW-Authenticate başlığı kontrolü - yalnızca yanıt commit edilmemişse değiştir
                    if (response.getHeaders().containsKey(HttpHeaders.WWW_AUTHENTICATE) && !response.isCommitted()) {
                        String authHeader = response.getHeaders().getFirst(HttpHeaders.WWW_AUTHENTICATE);
                        if (authHeader != null && authHeader.contains("Basic")) {
                            log.debug("Replacing WWW-Authenticate header value: {}", authHeader);
                            
                            // Mevcut başlığı temizlemek yerine yeni değerini ayarlamak daha güvenlidir
                            response.getHeaders().set(HttpHeaders.WWW_AUTHENTICATE, "Bearer realm=\"craftpilot\"");
                        }
                    }
                } catch (UnsupportedOperationException e) {
                    // Başlık değiştirilemiyorsa sadece bu durumu loglayalım
                    log.debug("Cannot modify WWW-Authenticate header: Response headers are read-only");
                }
                
                return Mono.empty();
            }));
    }

    @Override
    public int getOrder() {
        // Çoğu filtreden önce çalışsın
        return Ordered.HIGHEST_PRECEDENCE + 5;
    }
}
