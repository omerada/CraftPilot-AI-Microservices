package com.craftpilot.apigateway.security;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.ArrayList;

@Component
public class FirebaseAuthenticationFilter implements WebFilter {

    private static final Logger log = LoggerFactory.getLogger(FirebaseAuthenticationFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        
        // Public endpoints that don't require authentication
        if (isPublicEndpoint(path)) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String token = authHeader.substring(BEARER_PREFIX.length());

        return Mono.fromCallable(() -> FirebaseAuth.getInstance().verifyIdToken(token))
                .onErrorResume(e -> {
                    log.error("Firebase token verification failed", e);
                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                    return Mono.empty();
                })
                .flatMap(decodedToken -> authenticateAndChain(decodedToken, exchange, chain));
    }

    private boolean isPublicEndpoint(String path) {
        return path.startsWith("/public/") || 
               path.startsWith("/actuator/") ||
               path.equals("/");
    }

    private Mono<Void> authenticateAndChain(FirebaseToken decodedToken, 
                                          ServerWebExchange exchange, 
                                          WebFilterChain chain) {
        FirebaseUserDetails userDetails = new FirebaseUserDetails(decodedToken);
        UsernamePasswordAuthenticationToken authentication = 
            new UsernamePasswordAuthenticationToken(userDetails, null, new ArrayList<>());

        return chain.filter(exchange)
                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication));
    }
} 