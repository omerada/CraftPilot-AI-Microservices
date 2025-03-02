package com.craftpilot.apigateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;ContextHolder;
import org.springframework.stereotype.Component;.SecurityContext;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;er.ServerWebExchange;
import reactor.core.publisher.Mono;
@Component
public class AuthenticationLoggingFilter implements GlobalFilter, Ordered {
    private static final Logger log = LoggerFactory.getLogger(AuthenticationLoggingFilter.class);
    private static final Logger log = LoggerFactory.getLogger(AuthenticationLoggingFilter.class);
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();ayFilterChain chain) {
        String method = exchange.getRequest().getMethod().name();
        String method = exchange.getRequest().getMethod().name();
        log.debug("Request started: {} {}", method, path);
        log.debug("Request started: {} {}", method, path);
        // İstek tamamlandığında loglama
        exchange.getResponse().beforeCommit(() -> {
            HttpStatusCode statusCode = exchange.getResponse().getStatusCode();
            Boolean isAuthenticated = exchange.getAttribute("FIREBASE_AUTHENTICATED");
            Boolean isAuthenticated = exchange.getAttribute("FIREBASE_AUTHENTICATED");
            log.debug("Response status for {} {}: {} (Authenticated: {})",
                    method, path, statusCode, isAuthenticated != null ? isAuthenticated : "unknown");
                    method, path, statusCode, isAuthenticated != null ? isAuthenticated : "unknown");
            // 401 durumlarında tüm response başlıklarını loglayalım
            if (statusCode != null && statusCode.value() == 401) {
                log.debug("401 Response headers: {}", exchange.getResponse().getHeaders());
            }   log.debug("401 Response headers: {}", exchange.getResponse().getHeaders());
            }
            return Mono.empty();
        }); return Mono.empty();
        });
        // ReactiveSecurityContext'i alıp filtre zincirini devam ettir
        return ReactiveSecurityContextHolder.getContext()
            .doOnNext(securityContext -> {ext()
                if (securityContext != null && securityContext.getAuthentication() != null) {:getAuthentication)
                    log.debug("Current authentication: User={}, Authorities={}",rine bir Authentication nesnesi kullan
                            securityContext.getAuthentication().getName(),cation()) // Boş bir Authentication döndür
                            securityContext.getAuthentication().getAuthorities());
                } else {hentication) {
                    log.debug("No authentication found in context or authentication is null");text");
                }
            })orities={}",
            .then(chain.filter(exchange))           authentication.getName(), 
            // Security context bulunamazsa direkt filtre zincirini devam ettirAuthorities());
            .switchIfEmpty(chain.filter(exchange));  }
    }
       })
    @Override            .switchIfEmpty(chain.filter(exchange));
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 20;
    }
}
    // Boş Authentication sınıfı
    private static class EmptyAuthentication implements Authentication {
        @Override
        public String getName() {
            return "anonymous";
        }
        
        @Override
        public Object getCredentials() {
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
            // Yapma
        }
        
        @Override
        public java.util.Collection<? extends org.springframework.security.core.GrantedAuthority> getAuthorities() {
            return java.util.Collections.emptyList();
        }
        
        @Override
        public java.util.Map<String, Object> getAttributes() {
            return java.util.Collections.emptyMap();
        }
        
        @Override
        public Object getDetails() {
            return null;
        }
    }
}
