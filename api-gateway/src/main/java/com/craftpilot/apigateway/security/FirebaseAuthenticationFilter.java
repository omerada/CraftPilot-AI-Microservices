package com.craftpilot.apigateway.security;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Component
public class FirebaseAuthenticationFilter implements WebFilter {
    private static final Logger log = LoggerFactory.getLogger(FirebaseAuthenticationFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";
    private static final Set<String> PUBLIC_PATHS = Set.of(
        "/",
        "/swagger-ui.html",
        "/swagger-ui/**",
        "/v3/api-docs/**",
        "/webjars/**",
        "/actuator/health",
        "/actuator/info",
        "/api/auth/**",
        "/public/**"
    );
    
    private final FirebaseAuth firebaseAuth;
    private final Cache<String, FirebaseToken> tokenCache;

    public FirebaseAuthenticationFilter(FirebaseAuth firebaseAuth) {
        this.firebaseAuth = firebaseAuth;
        this.tokenCache = CacheBuilder.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .maximumSize(1000)
            .build();
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();

        // Use the same public paths from SecurityConfig
        if (isPublicPath(path)) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String token = authHeader.substring(7);

        return validateFirebaseToken(token)
            .flatMap(firebaseToken -> {
                // Firebase token'dan custom headers oluştur
                ServerWebExchange modifiedExchange = exchange.mutate()
                    .request(exchange.getRequest().mutate()
                        .header("X-User-Id", firebaseToken.getUid())
                        .header("X-User-Role", extractUserRole(firebaseToken))
                        .header("X-User-Email", firebaseToken.getEmail())
                        .build())
                    .build();
                
                return chain.filter(modifiedExchange);
            })
            .onErrorResume(e -> {
                log.error("Firebase authentication error", e);
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            });
    }

    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith) || 
               path.matches(".+\\.(png|jpg|ico|css|js|html)$");
    }

    private Mono<FirebaseToken> validateFirebaseToken(String token) {
        return Mono.fromCallable(() -> FirebaseAuth.getInstance().verifyIdToken(token));
    }

    private String extractUserRole(FirebaseToken token) {
        // Firebase custom claims'den rolleri al
        Object claims = token.getClaims().get("role");
        return claims != null ? claims.toString() : "USER";
    }
}