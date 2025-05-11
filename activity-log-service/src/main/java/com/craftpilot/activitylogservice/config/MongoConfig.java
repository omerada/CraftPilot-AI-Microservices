package com.craftpilot.activitylogservice.config;

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
@EnableReactiveMongoRepositories(basePackages = "com.craftpilot.activitylogservice.repository")
@EnableRetry
@Slf4j
public class MongoConfig extends AbstractReactiveMongoConfiguration {

    @Value("${spring.data.mongodb.uri}")
    private String mongoUri;

    @Value("${spring.data.mongodb.database}")
    private String database;

    @Value("${spring.data.mongodb.connection-timeout:10000}")
    private int connectionTimeout;
    
    @Value("${spring.data.mongodb.socket-timeout:30000}")
    private int socketTimeout;
    
    @Value("${spring.data.mongodb.max-connection-idle-time:60000}")
    private int maxConnectionIdleTime;
    
    @Value("${spring.data.mongodb.retry-writes:true}")
    private boolean retryWrites;
    
    @Value("${spring.data.mongodb.retry-reads:true}")
    private boolean retryReads;

    @Override
    protected String getDatabaseName() {
        return database;
    }

    @Override
    @Bean
    public MongoClient reactiveMongoClient() {
        log.info("Configuring MongoDB client with database: {}", database);
        log.info("MongoDB connection URI: {}", maskUri(mongoUri));
        
        try {
            ConnectionString connectionString = new ConnectionString(mongoUri);
            
            MongoClientSettings settings = MongoClientSettings.builder()
                    .applyConnectionString(connectionString)
                    .readPreference(ReadPreference.primary())
                    .writeConcern(WriteConcern.MAJORITY.withWTimeout(1000, TimeUnit.MILLISECONDS))
                    .applyToSocketSettings(builder -> 
                        builder.connectTimeout(connectionTimeout, TimeUnit.MILLISECONDS)
                               .readTimeout(socketTimeout, TimeUnit.MILLISECONDS))
                    .applyToConnectionPoolSettings(builder -> 
                        builder.maxConnectionIdleTime(maxConnectionIdleTime, TimeUnit.MILLISECONDS)
                               .maxWaitTime(20000, TimeUnit.MILLISECONDS)
                               .maxSize(20)
                               .minSize(5))
                    .retryWrites(retryWrites)
                    .retryReads(retryReads)
                    .build();

            return MongoClients.create(settings);
        } catch (Exception e) {
            log.error("Error creating MongoDB client: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Bean
    public ReactiveMongoTemplate reactiveMongoTemplate() throws Exception {
        try {
            log.info("Creating ReactiveMongoTemplate for database: {}", database);
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
