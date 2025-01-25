package com.craftpilot.apigateway.filter;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Objects;

/**
 * A custom Gateway filter named {@link JwtAuthenticationFilter} that handles JWT authentication for requests.
 * This filter validates JWT tokens for all requests except those to public endpoints.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends AbstractGatewayFilterFactory<JwtAuthenticationFilter.Config> {

    private final FirebaseAuth firebaseAuth;

    /**
     * Configuration class for JwtAuthenticationFilter.
     * It holds a list of public endpoints that should not be filtered.
     */
    public static class Config {
        // List of public endpoints that should not be filtered
        private List<String> publicEndpoints;

        /**
         * Gets the list of public endpoints.
         *
         * @return the list of public endpoints
         */
        public List<String> getPublicEndpoints() {
            return publicEndpoints;
        }

        /**
         * Sets the list of public endpoints.
         *
         * @param publicEndpoints the list of public endpoints to set
         * @return the updated Config object
         */
        public Config setPublicEndpoints(List<String> publicEndpoints) {
            this.publicEndpoints = publicEndpoints;
            return this;
        }

    }

    /**
     * Applies the JWT authentication filter to the gateway.
     *
     * @param config the configuration for the filter
     * @return the gateway filter
     */
    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String path = exchange.getRequest().getURI().getPath();

            // Public endpoint kontrolü
            if (config != null && config.getPublicEndpoints().stream().anyMatch(path::startsWith)) {
                return chain.filter(exchange);
            }

            String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

            // Auth header kontrolü
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }

            String token = authHeader.substring(7);

            return validateToken(token)
                    .flatMap(decodedToken -> {
                        // Token geçerliyse kullanıcı bilgilerini header'lara ekle
                        exchange.getRequest().mutate()
                                .header("X-User-ID", decodedToken.getUid())
                                .header("X-User-Email", decodedToken.getEmail())
                                .header("X-User-Role", Objects.toString(decodedToken.getClaims().get("role"), "USER"))
                                .build();

                        return chain.filter(exchange);
                    })
                    .onErrorResume(e -> {
                        log.error("Token doğrulama hatası: {}", e.getMessage());
                        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                        return exchange.getResponse().setComplete();
                    });
        };
    }

    @Cacheable(value = "tokenCache", key = "#token")
    private Mono<FirebaseToken> validateToken(String token) {
        return Mono.fromCallable(() -> firebaseAuth.verifyIdToken(token))
                .subscribeOn(Schedulers.boundedElastic());
    }

}