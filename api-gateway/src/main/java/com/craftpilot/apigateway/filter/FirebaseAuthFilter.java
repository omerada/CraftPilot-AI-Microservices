package com.craftpilot.apigateway.filter;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class FirebaseAuthFilter implements WebFilter {

    private final FirebaseAuth firebaseAuth;
    private static final String BEARER_PREFIX = "Bearer ";
    private static final List<String> PUBLIC_PATHS = List.of(
        "/actuator",
        "/api/public",
        "/auth/signup",
        "/auth/login",
        "/auth/refresh"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        
        // Filter'ın tek sefer çalışmasını sağlamak için özel bir header ekleyip kontrol ediyoruz
        if (exchange.getRequest().getHeaders().containsKey("X-Auth-Processed")) {
            return chain.filter(exchange);
        }
        
        log.debug("Processing request for path: {}", path);
        
        if (isPublicPath(path) || isPreflightRequest(exchange.getRequest())) {
            return chain.filter(exchange);
        }

        String token = extractToken(exchange.getRequest());
        if (token == null) {
            log.debug("No authorization token found");
            return handleError(exchange, HttpStatus.UNAUTHORIZED, "Yetkilendirme token'ı bulunamadı");
        }

        // Add CSRF token to response headers
        exchange.getResponse().getHeaders().add("X-CSRF-TOKEN", generateCsrfToken());
        
        return validateTokenAndAddHeaders(token, exchange, chain)
            .contextWrite(context -> ReactiveSecurityContextHolder.withAuthentication(
                new UsernamePasswordAuthenticationToken("user", null, Collections.emptyList())
            ));
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
        return Mono.fromCallable(() -> {
            try {
                return firebaseAuth.verifyIdToken(token);
            } catch (FirebaseAuthException e) {
                log.error("Token doğrulama hatası: {}", e.getMessage());
                throw new RuntimeException("Token doğrulama hatası: " + e.getMessage());
            }
        })
        .subscribeOn(Schedulers.boundedElastic())
        .flatMap(decodedToken -> {
            if (decodedToken != null) {
                log.debug("Token doğrulandı, kullanıcı: {}", decodedToken.getUid());
                
                // Orijinal Authorization header'ını al
                String originalAuth = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
                
                // Yeni bir request oluştur ve auth header'larını ekle
                ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                    .header("X-User-Id", decodedToken.getUid())
                    .header("X-User-Email", decodedToken.getEmail() != null ? decodedToken.getEmail() : "")
                    .header("X-User-Role", extractUserRole(decodedToken.getClaims()))
                    .header("X-Auth-Processed", "true")
                    // Orijinal Authorization header'ını AYNEN koru
                    .header(HttpHeaders.AUTHORIZATION, originalAuth)
                    .build();
                
                // Response header'larına CSRF token ve diğer bilgileri ekle
                exchange.getResponse().getHeaders().add("X-CSRF-TOKEN", generateCsrfToken());
                exchange.getResponse().getHeaders().remove(HttpHeaders.WWW_AUTHENTICATE);
                
                return chain.filter(exchange.mutate().request(mutatedRequest).build());
            }
            
            return handleError(exchange, HttpStatus.UNAUTHORIZED, "Geçersiz token");
        })
        .onErrorResume(e -> {
            log.error("Token işleme hatası: {}", e.getMessage());
            return handleError(exchange, HttpStatus.UNAUTHORIZED, "Token işleme hatası: " + e.getMessage());
        });
    }

    private String extractUserRole(Map<String, Object> claims) {
        Object roleObj = claims.get("role");
        return roleObj != null ? roleObj.toString() : "USER";
    }

    private Mono<Void> handleError(ServerWebExchange exchange, HttpStatus status, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().add(HttpHeaders.CONTENT_TYPE, "application/json;charset=UTF-8");
        
        String errorJson = String.format("{\"error\": \"%s\", \"status\": %d}", message, status.value());
        byte[] bytes = errorJson.getBytes(StandardCharsets.UTF_8);
        
        return response.writeWith(Mono.just(response.bufferFactory().wrap(bytes)));
    }

    private String generateCsrfToken() {
        return UUID.randomUUID().toString();
    }
}
