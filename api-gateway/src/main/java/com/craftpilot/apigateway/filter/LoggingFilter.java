package com.craftpilot.apigateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Component
@Order(-1)  // En önce çalışacak
public class LoggingFilter implements WebFilter {
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        long startTime = System.currentTimeMillis();
        String path = exchange.getRequest().getPath().value();
        String method = exchange.getRequest().getMethod().name();
        
        log.info("İstek başladı: {} {}", method, path);
        
        List<String> authHeaders = exchange.getRequest().getHeaders().get("Authorization");
        if (authHeaders != null) {
            log.debug("Authorization header mevcut: {}", 
                      authHeaders.get(0) != null ? "Evet (uzunluk: " + authHeaders.get(0).length() + ")" : "Hayır");
        } else {
            log.debug("Authorization header yok");
        }
        
        return chain.filter(exchange)
            .doOnSuccess(v -> {
                long duration = System.currentTimeMillis() - startTime;
                log.info("İstek tamamlandı: {} {} - {} - {} ms", 
                         method, path, exchange.getResponse().getStatusCode(), duration);
            })
            .doOnError(err -> {
                long duration = System.currentTimeMillis() - startTime;
                log.error("İstek hata ile sonuçlandı: {} {} - {} ms - Hata: {}", 
                          method, path, duration, err.getMessage());
            });
    }
}
