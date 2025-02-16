package com.craftpilot.apigateway.security;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
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

@Component
public class FirebaseAuthenticationFilter implements WebFilter {
    private static final Logger log = LoggerFactory.getLogger(FirebaseAuthenticationFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";
    
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

        return extractAndValidateToken(exchange)
            .flatMap(token -> authenticateAndChain(token, exchange, chain))
            .onErrorResume(this::handleError);
    }

    private boolean isPublicPath(String path) {
        return Arrays.stream(SecurityConfig.PUBLIC_PATHS).anyMatch(path::startsWith) || 
               path.matches(".+\\.(png|jpg|ico|css|js|html)$");
    }

    private Mono<FirebaseToken> extractAndValidateToken(ServerWebExchange exchange) {
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or invalid Authorization header"));
        }

        String token = authHeader.substring(BEARER_PREFIX.length());
        
        // Check cache first
        FirebaseToken cachedToken = tokenCache.getIfPresent(token);
        if (cachedToken != null) {
            return Mono.just(cachedToken);
        }

        // Verify token if not in cache
        return Mono.fromCallable(() -> {
            FirebaseToken verifiedToken = firebaseAuth.verifyIdToken(token);
            tokenCache.put(token, verifiedToken);
            return verifiedToken;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    private Mono<Void> authenticateAndChain(FirebaseToken decodedToken, 
                                          ServerWebExchange exchange, 
                                          WebFilterChain chain) {
        // Extract user roles and claims
        List<SimpleGrantedAuthority> authorities = extractAuthorities(decodedToken);
        
        // Create user details
        FirebaseUserDetails userDetails = new FirebaseUserDetails(decodedToken);
        
        // Create authentication token
        UsernamePasswordAuthenticationToken authentication = 
            new UsernamePasswordAuthenticationToken(userDetails, null, authorities);

        // Add custom headers
        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
            .header("X-User-Id", decodedToken.getUid())
            .header("X-User-Email", decodedToken.getEmail())
            .header("X-User-Role", String.join(",", 
                authorities.stream()
                    .map(SimpleGrantedAuthority::getAuthority)
                    .collect(Collectors.toList())))
            .build();

        // Create new exchange with mutated request
        ServerWebExchange mutatedExchange = exchange.mutate()
            .request(mutatedRequest)
            .build();

        return chain.filter(mutatedExchange)
            .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication));
    }

    private List<SimpleGrantedAuthority> extractAuthorities(FirebaseToken token) {
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        
        // Add default role
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        
        // Add custom roles from claims
        if (token.getClaims().containsKey("roles")) {
            @SuppressWarnings("unchecked")
            List<String> roles = (List<String>) token.getClaims().get("roles");
            roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                .forEach(authorities::add);
        }
        
        return authorities;
    }

    private Mono<Void> handleError(Throwable error) {
        if (error instanceof ResponseStatusException) {
            throw (ResponseStatusException) error;
        }
        
        log.error("Authentication error", error);
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication failed");
    }
}