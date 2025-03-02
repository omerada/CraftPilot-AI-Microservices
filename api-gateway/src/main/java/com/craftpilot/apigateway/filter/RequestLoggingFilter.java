package com.craftpilot.apigateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

public class RequestLoggingFilter implements WebFilter, Ordered {
    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();
        String method = request.getMethod().name();
        String requestId = request.getId();
        
        log.debug("[{}] >>> {} {} - Headers: {}", 
                requestId, 
                method, 
                path,
                request.getHeaders());
        
        // İsteğin başlangıç zamanını kaydet
        long startTime = System.currentTimeMillis();
        
        // İstek tamamlandıktan sonra logla
        return chain.filter(exchange)
            .doOnSuccess(aVoid -> {
                ServerHttpResponse response = exchange.getResponse();
                long duration = System.currentTimeMillis() - startTime;
                
                log.debug("[{}] <<< {} {} - Status: {} - Duration: {}ms - Headers: {}", 
                        requestId,
                        method, 
                        path,
                        response.getStatusCode(), 
                        duration,
                        response.getHeaders());
            })
            .doOnError(throwable -> {
                long duration = System.currentTimeMillis() - startTime;
                log.error("[{}] !!! {} {} - Error: {} - Duration: {}ms", 
                        requestId,
                        method, 
                        path,
                        throwable.getMessage(), 
                        duration);
            });
    }

    @Override
    public int getOrder() {
        // En ilk çalışması için
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
