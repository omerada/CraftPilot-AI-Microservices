package com.craftpilot.apigateway.security;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10) // Sıralamayı netleştir
public class FirebaseAuthenticationFilter implements WebFilter {
    
    private static final Logger log = LoggerFactory.getLogger(FirebaseAuthenticationFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";
    
    private final FirebaseAuth firebaseAuth;
    private final Cache<String, FirebaseToken> tokenCache = CacheBuilder.newBuilder()
        .expireAfterWrite(30, TimeUnit.MINUTES)
        .maximumSize(1000)
        .build();
    
    public FirebaseAuthenticationFilter(FirebaseAuth firebaseAuth) {
        this.firebaseAuth = firebaseAuth;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        // Request path bilgisini log'la
        String path = exchange.getRequest().getPath().value();
        String method = exchange.getRequest().getMethod().name();
        log.debug("Processing request: {} {}", method, path);
        
        // Actuator veya public path kontrolü
        if (SecurityConstants.isPublicPath(path) || path.startsWith("/actuator") || 
            path.contains("/health") || path.contains("/info") || "OPTIONS".equals(method)) {
            log.debug("Public path detected, skipping authentication: {}", path);
            return chain.filter(exchange);
        }

        // Auth header kontrolü
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            log.debug("Missing or invalid Authorization header for path: {}", path);
            return handleUnauthorized(exchange);
        }

        // Token doğrulama ve işleme
        String token = authHeader.substring(BEARER_PREFIX.length());
        return validateFirebaseToken(token)
            .flatMap(firebaseToken -> {
                log.debug("Token validated successfully for user: {}", firebaseToken.getUid());
                return processAuthenticatedRequest(exchange, chain, firebaseToken);
            })
            .onErrorResume(e -> {
                log.error("Authentication error: {}", e.getMessage());
                return handleUnauthorized(exchange);
            });
    }
    
    private Mono<FirebaseToken> validateFirebaseToken(String token) {
        // Cache kontrol et
        FirebaseToken cachedToken = tokenCache.getIfPresent(token);
        if (cachedToken != null) { 
            return Mono.just(cachedToken);
        }

        // Tokeni doğrula
        return Mono.fromCallable(() -> {
            try {
                FirebaseToken verifiedToken = firebaseAuth.verifyIdToken(token);
                tokenCache.put(token, verifiedToken);
                return verifiedToken;
            } catch (Exception e) {
                log.error("Token validation failed: {}", e.getMessage());
                throw new AuthenticationException("Invalid Firebase token: " + e.getMessage());
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    private Mono<Void> processAuthenticatedRequest(
            ServerWebExchange exchange,
            WebFilterChain chain,
            FirebaseToken firebaseToken) {
        
        FirebaseUserDetails userDetails = new FirebaseUserDetails(firebaseToken);
        FirebaseAuthenticationToken authToken = new FirebaseAuthenticationToken(userDetails, firebaseToken);
        
        // Exchange'i modifiye et
        ServerWebExchange modifiedExchange = modifyExchange(exchange, firebaseToken, userDetails);

        // Security context'i güncelle ve chain'i devam ettir
        return chain.filter(modifiedExchange)
            .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authToken));
    }

    private ServerWebExchange modifyExchange(ServerWebExchange exchange, FirebaseToken firebaseToken, FirebaseUserDetails userDetails) {
        String userId = firebaseToken.getUid();
        String role = userDetails.getRole();
        String email = firebaseToken.getEmail() != null ? firebaseToken.getEmail() : "no-email";

        log.debug("Setting user headers - ID: {}, Role: {}, Email: {}", userId, role, email);
        
        return exchange.mutate()
            .request(exchange.getRequest().mutate()
                .header("X-User-Id", userId)
                .header("X-User-Role", role) 
                .header("X-User-Email", email)
                .build())
            .build();
    }

    private Mono<Void> handleUnauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        
        // CORS başlıkları ekle
        String origin = exchange.getRequest().getHeaders().getOrigin();
        if (origin != null) {
            exchange.getResponse().getHeaders().set("Access-Control-Allow-Origin", origin);
            exchange.getResponse().getHeaders().set("Access-Control-Allow-Credentials", "true");
        }
        
        // WWW-Authenticate başlığını ayarla - SADECE Bearer
        exchange.getResponse().getHeaders().set("WWW-Authenticate", "Bearer realm=\"craftpilot\"");
        
        return exchange.getResponse().setComplete();
    }

    private static class AuthenticationException extends RuntimeException {
        public AuthenticationException(String message) {
            super(message);
        }
    }
}