package com.craftpilot.analyticsservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableRetry
@EnableAsync
@EnableScheduling
@Slf4j
public class RetryConfig {

    @Value("${mongodb.connection.retry.max-attempts:5}")
    private int maxAttempts;

    @Value("${mongodb.connection.retry.initial-interval:1000}")
    private long initialInterval;

    @Value("${mongodb.connection.retry.max-interval:30000}")
    private long maxInterval;

    @Value("${mongodb.connection.retry.multiplier:2.0}")
    private double multiplier;

    @Bean
    public RetryTemplate mongodbRetryTemplate() {
        log.info("Configuring MongoDB retry template with maxAttempts={}, initialInterval={}, maxInterval={}, multiplier={}",
                maxAttempts, initialInterval, maxInterval, multiplier);
                
        RetryTemplate retryTemplate = new RetryTemplate();
        
        // Backoff politikası - her denemede bekleme süresini artır
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(initialInterval);
        backOffPolicy.setMaxInterval(maxInterval);
        backOffPolicy.setMultiplier(multiplier);
        retryTemplate.setBackOffPolicy(backOffPolicy);
        
        // Yeniden deneme politikası - hangi hatalarda yeniden deneneceğini belirle
        Map<Class<? extends Throwable>, Boolean> retryableExceptions = new HashMap<>();
        retryableExceptions.put(com.mongodb.MongoTimeoutException.class, true);
        retryableExceptions.put(com.mongodb.MongoSocketReadTimeoutException.class, true);
        retryableExceptions.put(com.mongodb.MongoSocketOpenException.class, true);
        retryableExceptions.put(com.mongodb.MongoSocketException.class, true);
        retryableExceptions.put(java.net.ConnectException.class, true);
        retryableExceptions.put(java.net.SocketTimeoutException.class, true);
        retryableExceptions.put(org.springframework.dao.DataAccessResourceFailureException.class, true);
        
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(maxAttempts, retryableExceptions, true, true);
        retryTemplate.setRetryPolicy(retryPolicy);
        
        return retryTemplate;
    }
}
