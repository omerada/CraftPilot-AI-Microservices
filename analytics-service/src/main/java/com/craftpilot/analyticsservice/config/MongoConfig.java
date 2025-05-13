package com.craftpilot.analyticsservice.config;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractReactiveMongoConfiguration;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.annotation.Retryable;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableReactiveMongoRepositories(basePackages = "com.craftpilot.analyticsservice.repository")
@Slf4j
@EnableRetry
public class MongoConfig extends AbstractReactiveMongoConfiguration {

    @Value("${spring.data.mongodb.uri}")
    private String mongoUri;

    @Value("${spring.data.mongodb.database}")
    private String database;

    @Value("${spring.data.mongodb.username:craftpilotadmin}")
    private String username;

    @Value("${spring.data.mongodb.password:}")
    private String password;

    @Value("${spring.data.mongodb.authentication-database:admin}")
    private String authenticationDatabase;

    @Value("${spring.data.mongodb.connection-timeout:30000}")
    private int connectionTimeout;

    @Value("${spring.data.mongodb.socket-timeout:60000}")
    private int socketTimeout;

    @Value("${spring.data.mongodb.max-connection-idle-time:300000}")
    private int maxConnectionIdleTime;

    @Value("${spring.data.mongodb.connection-pool-max-size:50}")
    private int connectionPoolMaxSize;

    @Value("${spring.data.mongodb.connection-pool-min-size:5}")
    private int connectionPoolMinSize;

    @Value("${mongodb.connection.retry.max-attempts:5}")
    private int maxRetryAttempts;

    @Override
    protected String getDatabaseName() {
        return database;
    }

    @Override
    @Bean
    @Retryable(
        maxAttemptsExpression = "${mongodb.connection.retry.max-attempts:5}",
        backoff = @Backoff(delayExpression = "${mongodb.connection.retry.initial-interval:1000}", 
                          multiplierExpression = "${mongodb.connection.retry.multiplier:2.0}", 
                          maxDelayExpression = "${mongodb.connection.retry.max-interval:30000}")
    )
    public MongoClient reactiveMongoClient() {
        log.info("Initializing MongoDB client with database: {}", database);
        
        // Mask sensitive connection info for logging
        String maskedUri = mongoUri.replaceAll("mongodb://[^:]*:[^@]*@", "mongodb://***:***@");
        log.info("MongoDB URI (masked): {}", maskedUri);
        
        try {
            ConnectionString connectionString = new ConnectionString(mongoUri);
            String host = connectionString.getHosts().isEmpty() ? "unknown" : connectionString.getHosts().get(0);
            log.info("MongoDB host: {}", host);
            
            // Add more explicit logging for auth details (without exposing password)
            if (connectionString.getCredential() != null) {
                log.info("Auth source: {}, Username: {}", 
                    connectionString.getCredential().getSource(), 
                    connectionString.getCredential().getUserName());
            } else {
                log.warn("No credentials found in connection string!");
            }
            
            MongoClientSettings.Builder settingsBuilder = MongoClientSettings.builder()
                    .applyConnectionString(connectionString)
                    .applyToConnectionPoolSettings(builder -> 
                        builder.maxSize(connectionPoolMaxSize)
                               .minSize(connectionPoolMinSize)
                               .maxConnectionIdleTime(maxConnectionIdleTime, TimeUnit.MILLISECONDS)
                               .maxWaitTime(30000, TimeUnit.MILLISECONDS))
                    .applyToSocketSettings(builder -> 
                        builder.connectTimeout(connectionTimeout, TimeUnit.MILLISECONDS)
                               .readTimeout(socketTimeout, TimeUnit.MILLISECONDS))
                    .applyToServerSettings(builder ->
                        builder.heartbeatFrequency(10000, TimeUnit.MILLISECONDS));
            
            // Eğer URI'de kimlik bilgileri yoksa ve ayrıca tanımlanmışsa, ekle
            if (!mongoUri.contains("@") && username != null && !username.isEmpty() && password != null && !password.isEmpty()) {
                log.info("Adding credential from properties for user: {} on database: {}", username, authenticationDatabase);
                MongoCredential credential = MongoCredential.createCredential(
                    username, 
                    authenticationDatabase, 
                    password.toCharArray());
                settingsBuilder.credential(credential);
            }
            
            MongoClientSettings settings = settingsBuilder.build();
            log.info("MongoDB connection configured successfully");
            return MongoClients.create(settings);
        } catch (Exception e) {
            log.error("Failed to initialize MongoDB client: {}", e.getMessage(), e);
            throw e; // Rethrow to trigger retry mechanism
        }
    }

    @Bean
    public ReactiveMongoTemplate reactiveMongoTemplate() {
        try {
            return new ReactiveMongoTemplate(reactiveMongoClient(), getDatabaseName());
        } catch (Exception e) {
            log.error("Failed to create ReactiveMongoTemplate: {}", e.getMessage(), e);
            throw e;
        }
    }
}
