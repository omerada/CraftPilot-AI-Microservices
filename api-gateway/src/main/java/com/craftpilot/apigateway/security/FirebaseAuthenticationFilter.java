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
import com.craftpilot.apigateway.security.SecurityConstants;
import reactor.core.publisher.Mono;
import java.util.Set;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class FirebaseAuthenticationFilter implements WebFilter {
    
    private static final String BEARER_PREFIX = "Bearer ";

    private final FirebaseAuth firebaseAuth;
    private final Cache<String, FirebaseToken> tokenCache = CacheBuilder.newBuilder()
        .expireAfterWrite(5, TimeUnit.MINUTES)
        .maximumSize(1000)
        .build();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
 
        if (SecurityConstants.isPublicPath(path)) {
            return chain.filter(exchange);
        }

        return extractAndValidateToken(exchange)
            .flatMap(firebaseToken -> processAuthenticatedRequest(exchange, chain, firebaseToken))
            .onErrorResume(e -> handleAuthenticationError(exchange, e));
    }

    private Mono<FirebaseToken> extractAndValidateToken(ServerWebExchange exchange) {
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        
        log.debug("Auth header received: {}", authHeader); // Debug log

        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            log.error("Invalid auth header format: {}", authHeader); // Error log
            return Mono.error(new AuthenticationException("Invalid authorization header"));
        }

        String token = authHeader.substring(BEARER_PREFIX.length());
        log.debug("Extracted token (first 10 chars): {}", token.substring(0, Math.min(10, token.length()))); // Debug log
        return validateFirebaseToken(token);
    }

    private Mono<FirebaseToken> validateFirebaseToken(String token) {
        FirebaseToken cachedToken = tokenCache.getIfPresent(token);
        if (cachedToken != null) {
            log.debug("Using cached token for user: {}", cachedToken.getUid()); // Debug log
            return Mono.just(cachedToken);
        }

        return Mono.fromCallable(() -> {
            FirebaseToken verifiedToken = firebaseAuth.verifyIdToken(token);
            log.debug("Token verified successfully for user: {}", verifiedToken.getUid()); // Debug log
            tokenCache.put(token, verifiedToken);
            return verifiedToken;
        });
    }

    private Mono<Void> processAuthenticatedRequest(
            ServerWebExchange exchange,
            WebFilterChain chain,
            FirebaseToken firebaseToken) {
        
        // SecurityContext'e kullanıcıyı ekleyelim
        FirebaseUserDetails userDetails = new FirebaseUserDetails(firebaseToken);
        
        ServerWebExchange modifiedExchange = exchange.mutate()
            .request(exchange.getRequest().mutate()
                .header("X-User-Id", firebaseToken.getUid())
                .header("X-User-Role", extractUserRole(firebaseToken))
                .header("X-User-Email", firebaseToken.getEmail())
                .build())
            .build();

        log.debug("Added headers - UserId: {}, Role: {}, Email: {}", 
            firebaseToken.getUid(), 
            extractUserRole(firebaseToken), 
            firebaseToken.getEmail());

        return ReactiveSecurityContextHolder.getContext()
            .map(context -> {
                context.setAuthentication(new FirebaseAuthenticationToken(userDetails, firebaseToken));
                return context;
            })
            .then(chain.filter(modifiedExchange));
    }

    private Mono<Void> handleAuthenticationError(ServerWebExchange exchange, Throwable error) {
        log.error("Authentication error: {}", error.getMessage());
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    private boolean isPublicPath(String path) {
        // Exact matches for specific paths
        if (PUBLIC_PATHS.contains(path)) {
            return true;
        }
        
        // Check if path starts with any of the public paths
        // But ensure we're checking complete path segments
        return PUBLIC_PATHS.stream()
            .filter(publicPath -> publicPath.endsWith("/"))
            .anyMatch(publicPath -> {
                if (path.startsWith(publicPath)) {
                    // Check if the next character after the match is either end of string or '/'
                    int matchLength = publicPath.length();
                    return path.length() == matchLength || 
                           path.charAt(matchLength) == '/';
                }
                return false;
            }) || 
            path.matches(".+\\.(png|jpg|ico|css|js|html)$");
    }

    private String extractUserRole(FirebaseToken token) {
        Map<String, Object> claims = token.getClaims();
        
        // Rol bilgisi varsa al, yoksa USER olarak belirle
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