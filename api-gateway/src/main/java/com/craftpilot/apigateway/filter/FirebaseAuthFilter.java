package com.craftpilot.apigateway.filter;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class FirebaseAuthFilter implements WebFilter {

    private final FirebaseAuth firebaseAuth;
    private static final String BEARER_PREFIX = "Bearer ";
    private static final List<String> PUBLIC_PATHS = List.of(
        "/actuator",
        "/api/public",
        "/api/auth/signup",
        "/api/auth/login"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        
        if (isPublicPath(path) || isPreflightRequest(exchange.getRequest())) {
            return chain.filter(exchange);
        }

        String token = extractToken(exchange.getRequest());
        if (token == null) {
            return handleUnauthorized(exchange);
        }

        return validateTokenAndAddHeaders(token, exchange, chain);
    }

    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }

    private boolean isPreflightRequest(ServerHttpRequest request) {
        return request.getMethod() != null && 
               "OPTIONS".equals(request.getMethod().name());
    }

    private String extractToken(ServerHttpRequest request) {
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            return authHeader.substring(BEARER_PREFIX.length());
        }
        return null;
    }

    private Mono<Void> validateTokenAndAddHeaders(String token, ServerWebExchange exchange, WebFilterChain chain) {
        try {
            FirebaseToken decodedToken = verifyToken(token);
            if (decodedToken != null) {
                log.debug("Token verified for user: {}", decodedToken.getUid());
                
                ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                    .header("X-User-Id", decodedToken.getUid())
                    .header("X-User-Email", decodedToken.getEmail())
                    .header("X-User-Role", extractUserRole(decodedToken.getClaims()))
                    .build();

                return chain.filter(exchange.mutate().request(mutatedRequest).build());
            }
            log.error("Token verification returned null");
            return handleUnauthorized(exchange);
        } catch (Exception e) {
            log.error("Token validation failed with error: {}", e.getMessage(), e);
            return handleUnauthorized(exchange);
        }
    }

    private FirebaseToken verifyToken(String token) {
        try {
            return firebaseAuth.verifyIdToken(token);
        } catch (FirebaseAuthException e) {
            log.error("Firebase token verification failed: {}", e.getMessage(), e);
            return null;
        }
    }

    private String extractUserRole(Map<String, Object> claims) {
        Object roleObj = claims.get("role");
        return roleObj != null ? roleObj.toString() : "USER";
    }

    private Mono<Void> handleUnauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }
}
