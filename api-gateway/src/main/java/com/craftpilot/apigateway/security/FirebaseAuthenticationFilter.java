package com.craftpilot.apigateway.security;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import org.springframework.http.HttpHeaders;
import reactor.core.scheduler.Schedulers;
import org.springframework.http.HttpMethod;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class FirebaseAuthenticationFilter implements WebFilter {
    private static final Logger log = LoggerFactory.getLogger(FirebaseAuthenticationFilter.class);
    private final FirebaseAuth firebaseAuth;
    
    private final Cache<String, FirebaseToken> tokenCache = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.HOURS)
            .maximumSize(1000)
            .build();

    private static final List<String> PUBLIC_PATHS = Arrays.asList(
        "/actuator/",
        "/v3/api-docs",
        "/swagger-ui",
        "/webjars/",
        "/auth/login",
        "/auth/register",
        "/auth/reset-password"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        
        // Tekrar eden kontrolleri kaldır
        if (SecurityConstants.isPublicPath(path) || exchange.getRequest().getMethod() == HttpMethod.OPTIONS) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.debug("No valid authorization header found for path: {}", path);
            return handleAuthenticationError(exchange, 
                new AuthenticationException("Authorization header is missing or invalid"));
        }

        return extractAndValidateToken(exchange)
                .doOnError(error -> log.error("Token validation error: {}", error.getMessage()))
                .flatMap(token -> processAuthenticatedRequest(exchange, chain, token))
                .onErrorResume(AuthenticationException.class, 
                    error -> handleAuthenticationError(exchange, error))
                .onErrorResume(Exception.class, 
                    error -> {
                        log.error("Unexpected error during authentication", error);
                        return handleAuthenticationError(exchange, 
                            new AuthenticationException("Internal authentication error"));
                    });
    }

    private Mono<FirebaseToken> extractAndValidateToken(ServerWebExchange exchange) {
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return Mono.error(new AuthenticationException("Invalid authorization header"));
        }

        String token = authHeader.substring(7);
        return validateFirebaseToken(token);
    }

    private Mono<FirebaseToken> validateFirebaseToken(String token) {
        FirebaseToken cachedToken = tokenCache.getIfPresent(token);
        if (cachedToken != null) {
            return Mono.just(cachedToken);
        }

        return Mono.fromCallable(() -> {
            try {
                FirebaseToken verifiedToken = firebaseAuth.verifyIdToken(token);
                tokenCache.put(token, verifiedToken);
                return verifiedToken;
            } catch (Exception e) {
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
        
        ServerWebExchange modifiedExchange = modifyExchange(exchange, firebaseToken);

        return chain.filter(modifiedExchange)
                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authToken));
    }

    private ServerWebExchange modifyExchange(ServerWebExchange exchange, FirebaseToken firebaseToken) {
        return exchange.mutate()
                .request(exchange.getRequest().mutate()
                        .header("X-User-Id", firebaseToken.getUid())
                        .header("X-User-Role", extractUserRole(firebaseToken))
                        .header("X-User-Email", firebaseToken.getEmail() != null ? firebaseToken.getEmail() : "")
                        .build())
                .build();
    }

    private Mono<Void> handleAuthenticationError(ServerWebExchange exchange, AuthenticationException error) {
        log.error("Authentication error: {}", error.getMessage());
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().writeWith(Mono.just(exchange.getResponse()
            .bufferFactory().wrap(error.getMessage().getBytes())));
    }

    private String extractUserRole(FirebaseToken token) {
        Map<String, Object> claims = token.getClaims();
        
        if (claims.containsKey("role")) {
            return claims.get("role").toString();
        } else if (claims.containsKey("admin") && Boolean.TRUE.equals(claims.get("admin"))) {
            return "ADMIN";
        }
        return "USER";
    }

    private static class AuthenticationException extends RuntimeException {
        public AuthenticationException(String message) {
            super(message);
        }
    }
}