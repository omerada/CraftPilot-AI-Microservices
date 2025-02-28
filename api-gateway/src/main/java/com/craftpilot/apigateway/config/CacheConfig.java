package com.craftpilot.apigateway.config;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.firebase.auth.FirebaseToken;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.concurrent.TimeUnit;

@Configuration
public class CacheConfig {

    @Bean
    public Cache<String, FirebaseToken> tokenCache() {
        return CacheBuilder.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .maximumSize(1000)
                .build();
    }
}
