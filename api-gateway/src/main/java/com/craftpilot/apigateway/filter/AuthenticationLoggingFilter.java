package com.craftpilot.apigateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.Collections;

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
            HttpStatusCode statusCode = exchange.getResponse().getStatusCode();
            Boolean isAuthenticated = exchange.getAttribute("FIREBASE_AUTHENTICATED");
            
            log.debug("Response status for {} {}: {} (Authenticated: {})",
                    method, path, statusCode, isAuthenticated != null ? isAuthenticated : "unknown");
            
            // 401 durumlarında tüm response başlıklarını loglayalım
            if (statusCode != null && statusCode.value() == 401) {
                log.debug("401 Response headers: {}", exchange.getResponse().getHeaders());
            }
            
            return Mono.empty();
        });

        // Mevcut security context'i kontrol et ve logla
        return ReactiveSecurityContextHolder.getContext()
            .doOnNext(securityContext -> {
                // SecurityContext bulundu, Authentication nesnesi kontrol edilmeli
                if (securityContext != null && securityContext.getAuthentication() != null) {
                    Authentication auth = securityContext.getAuthentication();
                    log.debug("Current authentication: User={}, Authorities={}", 
                        auth.getName(),
                        auth.getAuthorities());
                } else {
                    log.debug("No authentication found in security context or authentication is null");
                }
            })
            .then(chain.filter(exchange))
            // SecurityContext bulunamadığında da filtre zincirini devam ettir
            .switchIfEmpty(Mono.defer(() -> {
                log.debug("No security context found");
                return chain.filter(exchange);
            }));
    }

    @Override
    public int getOrder() {
        // FirebaseAuthenticationFilter'dan sonra, ancak çoğu filtreden önce çalışsın
        return Ordered.HIGHEST_PRECEDENCE + 20;
    }
    
    // Boş Authentication için yardımcı sınıf gerekiyorsa
    private static class EmptyAuthentication implements Authentication {
        @Override
        public Collection<? extends GrantedAuthority> getAuthorities() {
            return Collections.emptyList();
        }

        @Override
        public Object getCredentials() {
            return null;
        }

        @Override
        public Object getDetails() {
            return null;
        }

        @Override
        public Object getPrincipal() {
            return "anonymous";
        }

        @Override
        public boolean isAuthenticated() {
            return false;
        }

        @Override
        public void setAuthenticated(boolean isAuthenticated) {
            // İşlem yapma
        }

        @Override
        public String getName() {
            return "anonymous";
        }
    }
}
