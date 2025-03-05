package com.craftpilot.apigateway.filter;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Collections;

@Slf4j
@RequiredArgsConstructor
public class FirebaseAuthFilter implements WebFilter {

    private final FirebaseAuth firebaseAuth;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return chain.filter(exchange);
        }

        String token = authHeader.substring(7);

        return Mono.fromCallable(() -> firebaseAuth.verifyIdToken(token))
            .onErrorResume(e -> {
                log.error("Firebase token verification failed: {}", e.getMessage());
                return Mono.empty();
            })
            .map(this::createAuthentication)
            .flatMap(auth -> chain.filter(exchange)
                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth)))
            .switchIfEmpty(chain.filter(exchange));
    }

    private UsernamePasswordAuthenticationToken createAuthentication(FirebaseToken token) {
        return new UsernamePasswordAuthenticationToken(
            token.getUid(),
            null,
            Collections.emptyList()
        );
    }
}
