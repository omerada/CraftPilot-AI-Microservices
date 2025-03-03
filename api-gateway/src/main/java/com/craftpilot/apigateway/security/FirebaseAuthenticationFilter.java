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
        
        if (SecurityConstants.isPublicPath(path)) {
            return chain.filter(exchange);
        }

        String token = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (token == null || !token.startsWith("Bearer ")) {
            return handleAuthenticationError(exchange, "Missing or invalid token");
        }

        token = token.substring(7);

        return Mono.fromCallable(() -> firebaseAuth.verifyIdToken(token))
            .subscribeOn(Schedulers.boundedElastic())
            .flatMap(decodedToken -> {
                FirebaseUserDetails userDetails = new FirebaseUserDetails(decodedToken);
                FirebaseAuthenticationToken auth = new FirebaseAuthenticationToken(userDetails, decodedToken);
                return chain.filter(exchange)
                    .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth));
            })
            .onErrorResume(e -> handleAuthenticationError(exchange, "Invalid token"));
    }

    private Mono<Void> handleAuthenticationError(ServerWebExchange exchange, String errorMessage) {
        log.error("Authentication error: {}", errorMessage);
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().writeWith(Mono.just(exchange.getResponse()
            .bufferFactory().wrap(errorMessage.getBytes())));
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