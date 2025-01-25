package com.craftpilot.userservice.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.Duration;

@Configuration
public class ServiceCircuitBreakerConfig {

    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        io.github.resilience4j.circuitbreaker.CircuitBreakerConfig config = 
            io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom()
                .failureRateThreshold(50)                // 50% hata oranında devreyi aç
                .waitDurationInOpenState(Duration.ofSeconds(10)) // 10 saniye bekle
                .slidingWindowSize(10)                   // Son 10 çağrıyı değerlendir
                .minimumNumberOfCalls(5)                 // Minimum 5 çağrı sonrası değerlendir
                .build();

        return CircuitBreakerRegistry.of(config);
    }
} 