package com.craftpilot.analyticsservice.config;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.ReadPreference;
import com.mongodb.WriteConcern;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractReactiveMongoConfiguration;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories;
import org.springframework.retry.annotation.EnableRetry;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableReactiveMongoRepositories(basePackages = "com.craftpilot.analyticsservice.repository")
@EnableRetry
@Slf4j
public class MongoConfig extends AbstractReactiveMongoConfiguration {

    @Value("${spring.data.mongodb.uri:mongodb://localhost:27017/analytics}")
    private String mongoUri;

    @Value("${spring.data.mongodb.host:${MONGODB_HOST:localhost}}")
    private String mongoHost;

    @Value("${spring.data.mongodb.port:${MONGODB_PORT:27017}}")
    private int mongoPort;

    @Value("${spring.data.mongodb.database:analytics}")
    private String databaseName;
    
    @Value("${spring.data.mongodb.connection-timeout:10000}")
    private int connectionTimeout;
    
    @Value("${spring.data.mongodb.socket-timeout:30000}")
    private int socketTimeout;
    
    @Value("${spring.data.mongodb.max-connection-idle-time:60000}")
    private int maxConnectionIdleTime;
    
    @Value("${spring.data.mongodb.max-retry-attempts:5}")
    private int maxRetryAttempts;
    
    @Value("${spring.data.mongodb.retry-interval:2000}")
    private long retryInterval;
    
    @Value("${spring.data.mongodb.server-selection-timeout:20000}")
    private int serverSelectionTimeout;

    @Override
    protected String getDatabaseName() {
        return databaseName;
    }

    @Override
    @Bean
    public MongoClient reactiveMongoClient() {
        log.info("Initializing MongoDB connection with host: {}, port: {}, database: {}", 
            mongoHost, mongoPort, databaseName);
        
        String effectiveMongoUri = mongoUri;
        
        // URI içinde host ve port bilgisi yoksa, ayrı değerlerden oluştur
        if (effectiveMongoUri.contains("localhost:27017") && 
            (!mongoHost.equals("localhost") || mongoPort != 27017)) {
            effectiveMongoUri = effectiveMongoUri.replace("localhost:27017", 
                mongoHost + ":" + mongoPort);
            log.info("Updated MongoDB URI with configured host and port: {}", 
                maskUri(effectiveMongoUri));
        }
        
        try {
            ConnectionString connectionString = new ConnectionString(effectiveMongoUri);
            
            MongoClientSettings settings = MongoClientSettings.builder()
                    .applyConnectionString(connectionString)
                    .readPreference(ReadPreference.primaryPreferred())
                    .writeConcern(WriteConcern.MAJORITY.withWTimeout(2000, TimeUnit.MILLISECONDS))
                    .applyToSocketSettings(builder -> 
                        builder.connectTimeout(connectionTimeout, TimeUnit.MILLISECONDS)
                               .readTimeout(socketTimeout, TimeUnit.MILLISECONDS))
                    .applyToConnectionPoolSettings(builder -> 
                        builder.maxConnectionIdleTime(maxConnectionIdleTime, TimeUnit.MILLISECONDS)
                               .maxWaitTime(20000, TimeUnit.MILLISECONDS)
                               .maxSize(20)
                               .minSize(5))
                    .applyToClusterSettings(builder -> 
                        builder.serverSelectionTimeout(serverSelectionTimeout, TimeUnit.MILLISECONDS))
                    .retryWrites(true)
                    .retryReads(true)
                    .build();

            log.info("MongoDB client configured successfully");
            return MongoClients.create(settings);
        } catch (Exception e) {
            log.error("Error creating MongoDB client: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Bean
    public ReactiveMongoTemplate reactiveMongoTemplate() throws Exception {
        try {
            log.info("Creating ReactiveMongoTemplate for database: {}", databaseName);
            return new ReactiveMongoTemplate(reactiveMongoClient(), getDatabaseName());
        } catch (Exception e) {
            log.error("Error creating ReactiveMongoTemplate: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    private String maskUri(String uri) {
        if (uri == null || uri.isEmpty()) {
            return "null-or-empty-uri";
        }
        try {
            // Temel bir maskeleme - şifreyi gizle
            return uri.replaceAll("(mongodb://[^:]+:)[^@]+(@.*)", "$1*******$2");
        } catch (Exception e) {
            return "invalid-uri-format";
        }
    }
}
