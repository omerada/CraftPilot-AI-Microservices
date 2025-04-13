package com.craftpilot.userservice.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

@Configuration
public class ResilienceConfig {

    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(20)                                    // Arttırıldı: 10 -> 20
                .failureRateThreshold(40.0f)                              // Düşürüldü: 50 -> 40
                .waitDurationInOpenState(Duration.ofSeconds(30))          // Arttırıldı: Açık durumda bekleme süresi
                .permittedNumberOfCallsInHalfOpenState(10)                // Arttırıldı: 5 -> 10
                .automaticTransitionFromOpenToHalfOpenEnabled(true)       // Otomatik geçiş etkinleştirildi
                .minimumNumberOfCalls(5)                                  // Minimum çağrı sayısı belirlendi
                .slowCallRateThreshold(50.0f)                             // Yavaş çağrı oranı eşiği
                .slowCallDurationThreshold(Duration.ofSeconds(5))         // Yavaş çağrı süresi eşiği
                // Tüm istisnalar kayıt altına alınıyor, ancak bazı istisna türlerini yoksayabiliriz
                .recordExceptions(Exception.class, TimeoutException.class)
                .build();
        
        return CircuitBreakerRegistry.of(circuitBreakerConfig);
    }

    @Bean
    public RetryRegistry retryRegistry() {
        RetryConfig retryConfig = RetryConfig.custom()
                .maxAttempts(5)                                          // Arttırıldı: 3 -> 5
                .waitDuration(Duration.ofMillis(500))                    // Düşürüldü: 1000 -> 500 (daha hızlı yeniden deneme)
                .retryExceptions(Exception.class)
                .ignoreExceptions(IllegalArgumentException.class)        // Bazı istisnaları yoksay
                .build();
        
        return RetryRegistry.of(retryConfig);
    }

    @Bean
    public CircuitBreaker userPreferencesCircuitBreaker(CircuitBreakerRegistry circuitBreakerRegistry) {
        return circuitBreakerRegistry.circuitBreaker("userPreferences");
    }
    
    @Bean
    public TimeLimiterConfig timeLimiterConfig() {
        return TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofSeconds(30))                 // Arttırıldı: Timeout süresi
                .cancelRunningFuture(true)
                .build();
    }
}
