package com.craftpilot.apigateway.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.core.Authentication;
import reactor.core.publisher.Mono;

@Configuration
public class AuthConfig {

    @Bean
    public ReactiveAuthenticationManager authenticationManager() {
        return new ReactiveAuthenticationManager() {
            @Override
            public Mono<Authentication> authenticate(Authentication authentication) {
                // Firebase tarafından doğrulanmış token'ı kabul et
                if (authentication instanceof FirebaseAuthenticationToken) {
                    authentication.setAuthenticated(true);
                    return Mono.just(authentication);
                }
                // Diğer auth token'ları reddet
                return Mono.empty();
            }
        };
    }
}
