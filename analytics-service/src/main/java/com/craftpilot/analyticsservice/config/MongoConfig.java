package com.craftpilot.analyticsservice.config;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractReactiveMongoConfiguration;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableReactiveMongoRepositories(basePackages = "com.craftpilot.analyticsservice.repository")
@EnableRetry
@Slf4j
public class MongoConfig extends AbstractReactiveMongoConfiguration {

    @Value("${spring.data.mongodb.uri}")
    private String mongoUri;

    @Value("${spring.data.mongodb.database:analytics}")
    private String databaseName;
    
    @Value("${spring.data.mongodb.connection-timeout:5000}")
    private int connectionTimeout;
    
    @Value("${spring.data.mongodb.max-retry-attempts:3}")
    private int maxRetryAttempts;

    @Override
    protected String getDatabaseName() {
        return databaseName;
    }

    @Bean
    public RetryTemplate retryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();
        
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(maxRetryAttempts);
        
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(500);
        backOffPolicy.setMultiplier(1.5);
        backOffPolicy.setMaxInterval(10000);
        
        retryTemplate.setRetryPolicy(retryPolicy);
        retryTemplate.setBackOffPolicy(backOffPolicy);
        
        return retryTemplate;
    }

    @Override
    @Bean
    public MongoClient reactiveMongoClient() {
        log.info("Initializing MongoDB connection with URI: {}", mongoUri.replaceAll("mongodb://.*@", "mongodb://****:****@"));
        
        try {
            ConnectionString connectionString = new ConnectionString(mongoUri);
            
            MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(connectionString)
                .applyToSocketSettings(builder -> 
                    builder.connectTimeout(connectionTimeout, TimeUnit.MILLISECONDS))
                .applyToClusterSettings(builder ->
                    builder.serverSelectionTimeout(connectionTimeout, TimeUnit.MILLISECONDS))
                .build();
                
            return MongoClients.create(settings);
        } catch (Exception e) {
            log.error("Failed to create MongoDB client: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Bean
    public ReactiveMongoTemplate reactiveMongoTemplate() {
        return new ReactiveMongoTemplate(reactiveMongoClient(), getDatabaseName());
    }
}
