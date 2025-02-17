package com.craftpilot.apigateway.security;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class FirebaseAuthenticationFilter implements WebFilter {
    
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
    private final Cache<String, FirebaseToken> tokenCache = CacheBuilder.newBuilder()
        .expireAfterWrite(5, TimeUnit.MINUTES)
        .maximumSize(1000)
        .build();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();

        if (isPublicPath(path)) {
            return chain.filter(exchange);
        }

        return extractAndValidateToken(exchange)
            .flatMap(firebaseToken -> processAuthenticatedRequest(exchange, chain, firebaseToken))
            .onErrorResume(e -> handleAuthenticationError(exchange, e));
    }

    private Mono<FirebaseToken> extractAndValidateToken(ServerWebExchange exchange) {
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            return Mono.error(new AuthenticationException("Invalid authorization header"));
        }

        String token = authHeader.substring(BEARER_PREFIX.length());
        return validateFirebaseToken(token);
    }

    private Mono<FirebaseToken> validateFirebaseToken(String token) {
        FirebaseToken cachedToken = tokenCache.getIfPresent(token);
        if (cachedToken != null) {
            return Mono.just(cachedToken);
        }

        return Mono.fromCallable(() -> {
            FirebaseToken verifiedToken = firebaseAuth.verifyIdToken(token);
            tokenCache.put(token, verifiedToken);
            return verifiedToken;
        });
    }

    private Mono<Void> processAuthenticatedRequest(
            ServerWebExchange exchange,
            WebFilterChain chain,
            FirebaseToken firebaseToken) {
        
        ServerWebExchange modifiedExchange = exchange.mutate()
            .request(exchange.getRequest().mutate()
                .header("X-User-Id", firebaseToken.getUid())
                .header("X-User-Role", extractUserRole(firebaseToken))
                .header("X-User-Email", firebaseToken.getEmail())
                .build())
            .build();

        return chain.filter(modifiedExchange);
    }

    private Mono<Void> handleAuthenticationError(ServerWebExchange exchange, Throwable error) {
        log.error("Authentication error: {}", error.getMessage());
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith) || 
               path.matches(".+\\.(png|jpg|ico|css|js|html)$");
    }

    private String extractUserRole(FirebaseToken token) {
        Object claims = token.getClaims().get("role");
        return claims != null ? claims.toString() : "USER";
    }

    private static class AuthenticationException extends RuntimeException {
        public AuthenticationException(String message) {
            super(message);
        }
    }
}