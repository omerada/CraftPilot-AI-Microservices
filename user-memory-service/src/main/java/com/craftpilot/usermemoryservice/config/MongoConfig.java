package com.craftpilot.usermemoryservice.config;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.connection.SslSettings;
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
@EnableReactiveMongoRepositories(basePackages = "com.craftpilot.usermemoryservice.repository")
@Slf4j
public class MongoConfig extends AbstractReactiveMongoConfiguration {

    @Value("${spring.data.mongodb.uri}")
    private String mongoUri;

    @Value("${spring.data.mongodb.database}")
    private String database;

    @Override
    protected String getDatabaseName() {
        return database;
    }

    @Override
    @Bean
    public MongoClient reactiveMongoClient() {
        log.info("Initializing MongoDB connection with URI: {}", mongoUri.replaceAll("mongodb://.*@", "mongodb://****:****@"));
        
        ConnectionString connectionString = new ConnectionString(mongoUri);
        
        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(connectionString)
                .applyToClusterSettings(builder -> 
                    builder.serverSelectionTimeout(5000, TimeUnit.MILLISECONDS))
                .applyToConnectionPoolSettings(builder ->
                    builder.maxConnectionIdleTime(30000, TimeUnit.MILLISECONDS)
                           .maxSize(50))
                .applyToSocketSettings(builder ->
                    builder.connectTimeout(5000, TimeUnit.MILLISECONDS)
                           .readTimeout(10000, TimeUnit.MILLISECONDS))
                .build();
        
        return MongoClients.create(settings);
    }
    
    @Bean
    public ReactiveMongoTemplate reactiveMongoTemplate() {
        return new ReactiveMongoTemplate(reactiveMongoClient(), getDatabaseName());
    }
}
