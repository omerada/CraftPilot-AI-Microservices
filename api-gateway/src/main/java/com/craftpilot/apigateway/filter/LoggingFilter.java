package com.craftpilot.apigateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class LoggingFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(LoggingFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long startTime = System.currentTimeMillis();
        String path = exchange.getRequest().getPath().value();
        String method = exchange.getRequest().getMethod().name();
        
        log.debug(">>> {} {}", method, path);
        
        return chain.filter(exchange).doFinally(signalType -> {
            long duration = System.currentTimeMillis() - startTime;
            log.debug("<<< {} {} - {}, took {}ms", 
                method, path, exchange.getResponse().getStatusCode(), duration);
        });
    }

    @Override
    public int getOrder() {
        // LoggingFilter en önce çalışsın
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }
}
