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
        return Mono.fromCallable(() -> verifyToken(token))
            .subscribeOn(Schedulers.boundedElastic())
            .flatMap(decodedToken -> {
                if (decodedToken != null) {
                    ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                        .headers(headers -> addUserHeaders(headers, decodedToken))
                        .build();

                    return chain.filter(exchange.mutate().request(mutatedRequest).build());
                }
                return handleUnauthorized(exchange);
            })
            .onErrorResume(e -> {
                log.error("Token validation error: {}", e.getMessage());
                return handleUnauthorized(exchange);
            });
    }

    private FirebaseToken verifyToken(String token) {
        try {
            return firebaseAuth.verifyIdToken(token);
        } catch (FirebaseAuthException e) {
            log.error("Firebase token verification failed: {}", e.getMessage());
            return null;
        }
    }

    private void addUserHeaders(HttpHeaders headers, FirebaseToken token) {
        // Clear existing auth-related headers
        headers.remove(HttpHeaders.AUTHORIZATION);
        headers.remove("X-User-Id");
        headers.remove("X-User-Email");
        headers.remove("X-User-Role");
        
        // Add new header values
        Map<String, Object> claims = token.getClaims();
        headers.add("X-User-Id", token.getUid());
        headers.add("X-User-Email", token.getEmail());
        headers.add("X-User-Role", extractUserRole(claims));
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
