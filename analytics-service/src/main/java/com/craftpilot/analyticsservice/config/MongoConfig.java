package com.craftpilot.analyticsservice.config;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractReactiveMongoConfiguration;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableReactiveMongoRepositories(basePackages = "com.craftpilot.analyticsservice.repository")
@Slf4j
public class MongoConfig extends AbstractReactiveMongoConfiguration {

    @Value("${spring.data.mongodb.uri}")
    private String mongoUri;

    @Value("${spring.data.mongodb.database}")
    private String database;

    @Value("${spring.data.mongodb.connection-timeout:30000}")
    private int connectionTimeout;

    @Value("${spring.data.mongodb.socket-timeout:60000}")
    private int socketTimeout;

    @Value("${spring.data.mongodb.max-connection-idle-time:300000}")
    private int maxConnectionIdleTime;

    @Value("${mongodb.connection.retry.max-attempts:10}")
    private int maxRetryAttempts;

    @Override
    protected String getDatabaseName() {
        return database;
    }

    @Override
    @Bean
    public MongoClient reactiveMongoClient() {
        log.info("Initializing MongoDB client with database: {}", database);
        
        try {
            ConnectionString connectionString = new ConnectionString(mongoUri);
            
            MongoClientSettings settings = MongoClientSettings.builder()
                    .applyConnectionString(connectionString)
                    .applyToConnectionPoolSettings(builder -> 
                        builder.maxSize(50)
                               .minSize(5)
                               .maxConnectionIdleTime(maxConnectionIdleTime, TimeUnit.MILLISECONDS)
                               .maxWaitTime(10000, TimeUnit.MILLISECONDS))
                    .applyToSocketSettings(builder -> 
                        builder.connectTimeout(connectionTimeout, TimeUnit.MILLISECONDS)
                               .readTimeout(socketTimeout, TimeUnit.MILLISECONDS))
                    .applyToServerSettings(builder ->
                        builder.heartbeatFrequency(20000, TimeUnit.MILLISECONDS))
                    .build();
            
            log.info("MongoDB connection configured successfully");
            return MongoClients.create(settings);
        } catch (Exception e) {
            log.error("Failed to initialize MongoDB client: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Bean
    public ReactiveMongoTemplate reactiveMongoTemplate() {
        return new ReactiveMongoTemplate(reactiveMongoClient(), getDatabaseName());
    }
}
