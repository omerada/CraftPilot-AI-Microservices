package com.craftpilot.apigateway.security;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import org.springframework.http.HttpHeaders;
import reactor.core.scheduler.Schedulers;

@Slf4j
@Component
@RequiredArgsConstructor
public class FirebaseAuthenticationFilter implements WebFilter {
    private final FirebaseAuth firebaseAuth;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return chain.filter(exchange);
        }

        String token = authHeader.substring(7);

        return Mono.fromCallable(() -> firebaseAuth.verifyIdToken(token))
            .subscribeOn(Schedulers.boundedElastic())
            .map(this::createAuthentication)
            .flatMap(authentication -> chain.filter(exchange)
                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication)))
            .onErrorResume(e -> {
                log.error("Error processing Firebase token", e);
                return chain.filter(exchange);
            });
    }

    private Authentication createAuthentication(FirebaseToken firebaseToken) {
        FirebaseUserDetails userDetails = new FirebaseUserDetails(firebaseToken);

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
        
        // Exchange'i modifiye et
        ServerWebExchange modifiedExchange = modifyExchange(exchange, firebaseToken);

        // Security context'i güncelle ve chain'i devam ettir
        return chain.filter(modifiedExchange)
            .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authToken));
    }

    private ServerWebExchange modifyExchange(ServerWebExchange exchange, FirebaseToken firebaseToken) {
        return exchange.mutate()
            .request(exchange.getRequest().mutate()
                .header("X-User-Id", firebaseToken.getUid())
                .header("X-User-Role", extractUserRole(firebaseToken))
                .header("X-User-Email", firebaseToken.getEmail())
                .build())
            .build();
    }

    private Mono<Void> handleAuthenticationError(ServerWebExchange exchange, Throwable error) {
        log.error("Authentication error: {}", error.getMessage());
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
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