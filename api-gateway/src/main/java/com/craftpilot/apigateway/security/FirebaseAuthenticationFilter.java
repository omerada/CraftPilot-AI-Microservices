package com.craftpilot.apigateway.security;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.server.context.ServerSecurityContextRepository;
import org.springframework.security.web.server.context.WebSessionServerSecurityContextRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.concurrent.TimeUnit;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class FirebaseAuthenticationFilter implements WebFilter {

    private static final Logger log = LoggerFactory.getLogger(FirebaseAuthenticationFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";

    private final FirebaseAuth firebaseAuth;
    private final Cache<String, FirebaseToken> tokenCache = CacheBuilder.newBuilder()
            .expireAfterWrite(30, TimeUnit.MINUTES)
            .maximumSize(1000)
            .build();
    private final ServerSecurityContextRepository securityContextRepository;
    private final ReactiveAuthenticationManager authManager;

    public FirebaseAuthenticationFilter(FirebaseAuth firebaseAuth, 
                                       ReactiveAuthenticationManager authManager) {
        this.firebaseAuth = firebaseAuth;
        this.authManager = authManager;
        this.securityContextRepository = new WebSessionServerSecurityContextRepository();
    }

    // Filtre işlevinden önce verilen path için kimlik doğrulaması gerekip gerekmediğini kontrol et
    private boolean shouldAuthenticate(String path, String method) {
        boolean isPublic = SecurityConstants.isPublicPath(path) ||
                path.startsWith("/actuator") ||
                path.contains("/health") ||
                path.contains("/info") ||
                "OPTIONS".equals(method);

        if (isPublic) {
            log.debug("Public path detected, authentication not required: {} {}", method, path);
            return false;
        }
        return true;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        // Loglama işlemi
        String path = exchange.getRequest().getPath().value();
        String method = exchange.getRequest().getMethod().name();
        log.debug("Processing request: {} {}", method, path);

        // Response tamamlandığında başlıkları kontrol et
        exchange.getResponse().beforeCommit(() -> {
            try {
                ServerHttpResponse response = exchange.getResponse();
                if (!response.isCommitted()) {
                    HttpHeaders headers = response.getHeaders();
                    
                    // HTTP Basic kimlik doğrulama başlığı varsa düzelt
                    if (headers.containsKey(HttpHeaders.WWW_AUTHENTICATE)) {
                        String authHeader = headers.getFirst(HttpHeaders.WWW_AUTHENTICATE);
                        if (authHeader != null && authHeader.contains("Basic")) {
                            log.debug("Converting Basic auth to Bearer auth header");
                            headers.set(HttpHeaders.WWW_AUTHENTICATE, "Bearer realm=\"craftpilot\"");
                        }
                    }
                }
            } catch (Exception e) {
                // Başlık değiştirilemiyorsa yoksay
                log.debug("Exception while setting WWW-Authenticate header (safe to ignore): {}", e.getMessage());
            }
            return Mono.empty();
        });

        // İsteğin zaten filtrelenip filtrelenmediğini kontrol eden bir attribute ekleyelim
        if (exchange.getAttribute("FIREBASE_FILTERED") != null) {
            log.debug("Request already filtered, skipping duplicate processing");
            return chain.filter(exchange);
        }
        exchange.getAttributes().put("FIREBASE_FILTERED", Boolean.TRUE);

        // Kimlik doğrulama gerektirmeyen path'lerin filtreden geçmesine izin ver
        if (!shouldAuthenticate(path, method)) {
            return chain.filter(exchange);
        }

        // Auth header kontrolü
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            log.debug("Missing or invalid Authorization header for path: {}", path);
            return handleUnauthorized(exchange);
        }

        // Token doğrulama ve işleme
        String token = authHeader.substring(BEARER_PREFIX.length());
        log.debug("Validating token for path: {}", path);

        // Orijinal token'ı processAuthenticatedRequest metoduna aktarıyoruz
        return validateFirebaseToken(token)
                .flatMap(firebaseToken -> {
                    log.debug("Token validated successfully for user: {}", firebaseToken.getUid());
                    return processAuthenticatedRequest(exchange, chain, firebaseToken, token);
                })
                .onErrorResume(e -> {
                    log.error("Authentication error for path {}: {}", path, e.getMessage());
                    return handleUnauthorized(exchange);
                });
    }

    private Mono<FirebaseToken> validateFirebaseToken(String token) {
        // Cache kontrol et
        FirebaseToken cachedToken = tokenCache.getIfPresent(token);
        if (cachedToken != null) { 
            log.debug("Using cached token for user: {}", cachedToken.getUid());
            return Mono.just(cachedToken);
        }
        
        // Tokeni doğrula
        return Mono.fromCallable(() -> {
            try {
                log.debug("Validating token with Firebase Auth service");
                FirebaseToken verifiedToken = firebaseAuth.verifyIdToken(token);
                log.debug("Token validation successful for user: {}", verifiedToken.getUid());
                tokenCache.put(token, verifiedToken);
                return verifiedToken;
            } catch (Exception e) {
                log.error("Token validation failed with error: {}", e.getMessage());
                // Stack trace'i de loglayalım
                log.debug("Token validation error details", e);
                throw new AuthenticationException("Invalid Firebase token: " + e.getMessage());
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    private Mono<Void> processAuthenticatedRequest(
            ServerWebExchange exchange,
            WebFilterChain chain,
            FirebaseToken firebaseToken,
            String originalToken) {  
        
        try {
            FirebaseUserDetails userDetails = new FirebaseUserDetails(firebaseToken);
            FirebaseAuthenticationToken authToken = new FirebaseAuthenticationToken(userDetails, firebaseToken);
            
            // Debug için authToken bilgilerini logla
            log.debug("Created authentication token for user: {} with roles: {}", 
                    userDetails.getUid(), 
                    userDetails.getAuthorities().stream()
                             .map(auth -> auth.getAuthority())
                             .reduce("", (a, b) -> a + "," + b));
            
            // Exchange'i modifiye et - orijinal token'ı da aktarıyoruz
            ServerWebExchange modifiedExchange = modifyExchange(exchange, firebaseToken, userDetails, originalToken);
            
            // Bu attribute, diğer filtrelerde kullanmak için eklenmiştir
            modifiedExchange.getAttributes().put("FIREBASE_AUTHENTICATED", Boolean.TRUE);
            
            // SecurityContext'i oluştur ve kaydet
            SecurityContextImpl securityContext = new SecurityContextImpl(authToken);
            
            // SecurityContext'i ReactiveSecurityContextHolder'a yaz VE repository'ye kaydet
            return this.securityContextRepository.save(exchange, securityContext)
                .then(chain.filter(modifiedExchange)
                    .contextWrite(context -> {
                        log.debug("Writing authentication to ReactiveSecurityContextHolder");
                        return ReactiveSecurityContextHolder.withAuthentication(authToken);
                    }))
                .doOnSuccess(v -> log.debug("Filter chain completed successfully"))
                .doOnError(e -> log.error("Error in filter chain: {}", e.getMessage(), e));
                
        } catch (Exception e) {
            log.error("Exception in processAuthenticatedRequest: {}", e.getMessage(), e);
            return handleUnauthorized(exchange);
        }
    }

    private ServerWebExchange modifyExchange(
            ServerWebExchange exchange, 
            FirebaseToken firebaseToken, 
            FirebaseUserDetails userDetails,
            String originalToken) {  
        
        String userId = firebaseToken.getUid();
        String role = userDetails.getRole();
        String email = firebaseToken.getEmail() != null ? firebaseToken.getEmail() : "no-email";
        
        log.debug("Setting user headers - ID: {}, Role: {}, Email: {}", userId, role, email);
        
        // İsteğe kullanıcı bilgilerini ekle
        return exchange.mutate()
            .request(exchange.getRequest().mutate()
                .header("X-User-Id", userId)
                .header("X-User-Role", role)
                .header("X-User-Email", email)
                // Orijinal token'ı kullanıyoruz
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + originalToken)
                .build())
            .build();
    }

    private Mono<Void> handleUnauthorized(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        
        try {
            // CORS başlıkları ekle - response commit edilmemişse
            if (!response.isCommitted()) {
                String origin = exchange.getRequest().getHeaders().getOrigin();
                if (origin != null) {
                    response.getHeaders().set("Access-Control-Allow-Origin", origin);
                    response.getHeaders().set("Access-Control-Allow-Credentials", "true");
                }
                
                // WWW-Authenticate başlığını ayarla - Basic kısmını tamamen kaldır
                response.getHeaders().remove(HttpHeaders.WWW_AUTHENTICATE);  // Önce mevcut başlığı temizle
                response.getHeaders().set(HttpHeaders.WWW_AUTHENTICATE, "Bearer realm=\"craftpilot\"");
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
            }
            
            // Unauthorized durumda yeterince detaylı log
            log.debug("Returning 401 Unauthorized response for path: {}", 
                    exchange.getRequest().getPath().value());
        } catch (UnsupportedOperationException e) {
            // Başlıklar değiştirilemiyorsa (muhtemelen yanıt commit edildiği için)
            log.debug("Cannot set response headers: {}", e.getMessage());
        }
        
        return response.setComplete();
    }

    private static class AuthenticationException extends RuntimeException {
        public AuthenticationException(String message) {
            super(message);
        }
    }
}