package com.craftpilot.userservice.config;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.ReadPreference;
import com.mongodb.WriteConcern;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractReactiveMongoConfiguration;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories;

import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;

@Configuration
@EnableReactiveMongoRepositories(basePackages = "com.craftpilot.userservice.repository")
@Slf4j
public class MongoConfig extends AbstractReactiveMongoConfiguration {

    @Value("${spring.data.mongodb.uri}")
    private String mongoUri;
    
    @Value("${spring.data.mongodb.database:craftpilot}")
    private String database;
    
    @Value("${spring.data.mongodb.connection-timeout:30000}")
    private int connectionTimeout;
    
    @Value("${spring.data.mongodb.socket-timeout:60000}")
    private int socketTimeout;
    
    @Value("${spring.data.mongodb.max-connection-idle-time:300000}")
    private int maxConnectionIdleTime;
    
    @Value("${spring.data.mongodb.connection-pool-max-size:20}")
    private int connectionPoolMaxSize;
    
    @Override
    protected String getDatabaseName() {
        return database;
    }
    
    @Override
    @Bean
    public MongoClient reactiveMongoClient() {
        log.info("Configuring MongoDB client with URI: {}", maskUri(mongoUri));
        
        ConnectionString connectionString = new ConnectionString(mongoUri);
        
        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(connectionString)
                .readPreference(ReadPreference.primaryPreferred())
                .writeConcern(WriteConcern.MAJORITY.withWTimeout(2000, TimeUnit.MILLISECONDS))
                .applyToConnectionPoolSettings(builder -> 
                        builder.maxSize(connectionPoolMaxSize)
                               .minSize(5)
                               .maxConnectionIdleTime(maxConnectionIdleTime, TimeUnit.MILLISECONDS)
                               .maxWaitTime(20000, TimeUnit.MILLISECONDS))
                .applyToSocketSettings(builder -> 
                        builder.connectTimeout(connectionTimeout, TimeUnit.MILLISECONDS)
                               .readTimeout(socketTimeout, TimeUnit.MILLISECONDS))
                .retryWrites(true)
                .retryReads(true)
                .build();
        
        log.info("MongoDB client configured successfully");
        return MongoClients.create(settings);
    }
    
    @Bean
    public ReactiveMongoTemplate reactiveMongoTemplate() {
        return new ReactiveMongoTemplate(reactiveMongoClient(), getDatabaseName());
    }
    
    private String maskUri(String uri) {
        if (uri == null || uri.isEmpty()) {
            return "null-or-empty-uri";
        }
        try {
            // Basic masking - hide password
            return uri.replaceAll("(mongodb://[^:]+:)[^@]+(@.*)", "$1*******$2");
        } catch (Exception e) {
            return "invalid-uri-format";
        }
    }
}
