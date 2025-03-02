package com.craftpilot.apigateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class AuthenticationLoggingFilter implements GlobalFilter, Ordered {
    private static final Logger log = LoggerFactory.getLogger(AuthenticationLoggingFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        String method = exchange.getRequest().getMethod().name();
        
        log.debug("Request started: {} {}", method, path);
        
        // İstek tamamlandığında loglama
        exchange.getResponse().beforeCommit(() -> {
            HttpStatus status = exchange.getResponse().getStatusCode();
            Boolean isAuthenticated = exchange.getAttribute("FIREBASE_AUTHENTICATED");
            
            log.debug("Response status for {} {}: {} (Authenticated: {})",
                    method, path, status, isAuthenticated != null ? isAuthenticated : "unknown");
            
            // 401 durumlarında tüm response başlıklarını loglayalım
            if (status == HttpStatus.UNAUTHORIZED) {
                log.debug("401 Response headers: {}", exchange.getResponse().getHeaders());
            }
            
            return Mono.empty();
        });

        // Mevcut security context'i kontrol et ve logla
        return ReactiveSecurityContextHolder.getContext()
            .map(SecurityContext::getAuthentication)
            .defaultIfEmpty(null)
            .flatMap(authentication -> {
                if (authentication != null) {
                    log.debug("Current authentication: User={}, Authorities={}",
                            authentication.getName(), 
                            authentication.getAuthorities());
                } else {
                    log.debug("No authentication found in context");
                }
                return chain.filter(exchange);
            })
            .switchIfEmpty(chain.filter(exchange));
    }

    @Override
    public int getOrder() {
        // FirebaseAuthenticationFilter'dan sonra, ancak çoğu filtreden önce çalışsın
        return Ordered.HIGHEST_PRECEDENCE + 20;
    }
}
