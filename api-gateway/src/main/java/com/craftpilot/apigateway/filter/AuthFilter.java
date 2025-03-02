package com.craftpilot.apigateway.filter;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class AuthFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(AuthFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";
    private static final List<String> PUBLIC_PATHS = Arrays.asList("/auth/", "/actuator/");

    private final FirebaseAuth firebaseAuth;
    private final Cache<String, FirebaseToken> tokenCache;

    public AuthFilter(FirebaseAuth firebaseAuth) {
        this.firebaseAuth = firebaseAuth;
        this.tokenCache = CacheBuilder.newBuilder()
                .expireAfterWrite(30, TimeUnit.MINUTES)
                .maximumSize(1000)
                .build();
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        
        // Public pathler için doğrulama yok
        if (isPublicPath(path)) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String token = authHeader.substring(BEARER_PREFIX.length());
        return validateToken(token)
            .flatMap(firebaseToken -> {
                ServerHttpRequest modifiedRequest = exchange.getRequest().mutate()
                    .header("X-User-Id", firebaseToken.getUid())
                    .header("X-User-Email", firebaseToken.getEmail())
                    .header("X-User-Role", getUserRole(firebaseToken))
                    .build();
                
                return chain.filter(exchange.mutate().request(modifiedRequest).build());
            })
            .onErrorResume(e -> {
                log.error("Authentication failed: {}", e.getMessage());
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            });
    }

    private Mono<FirebaseToken> validateToken(String token) {
        FirebaseToken cachedToken = tokenCache.getIfPresent(token);
        if (cachedToken != null) {
            return Mono.just(cachedToken);
        }
        
        return Mono.fromCallable(() -> {
            FirebaseToken verifiedToken = firebaseAuth.verifyIdToken(token);
            tokenCache.put(token, verifiedToken);
            return verifiedToken;
        }).subscribeOn(Schedulers.boundedElastic());
    }
    
    private String getUserRole(FirebaseToken token) {
        // Basit rol mantığı
        return "USER";
    }
    
    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
