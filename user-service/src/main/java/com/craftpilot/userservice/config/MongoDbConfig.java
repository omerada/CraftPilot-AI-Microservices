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

@Configuration
@EnableReactiveMongoRepositories(basePackages = "com.craftpilot.userservice.repository")
public class MongoDbConfig extends AbstractReactiveMongoConfiguration {

    @Value("${spring.data.mongodb.uri}")
    private String mongoUri;
    
    @Value("${spring.data.mongodb.database:craftpilot}")
    private String database;
    
    @Override
    protected String getDatabaseName() {
        return database;
    }
    
    @Override
    @Bean
    public MongoClient reactiveMongoClient() {
        ConnectionString connectionString = new ConnectionString(mongoUri);
        
        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(connectionString)
                .readPreference(ReadPreference.primaryPreferred())
                .writeConcern(WriteConcern.MAJORITY.withWTimeout(1000, TimeUnit.MILLISECONDS))
                .applyToConnectionPoolSettings(builder -> 
                        builder.maxConnectionIdleTime(30000, TimeUnit.MILLISECONDS)
                               .maxSize(20)
                               .minSize(5))
                .applyToSocketSettings(builder -> 
                        builder.connectTimeout(10000, TimeUnit.MILLISECONDS)
                               .readTimeout(15000, TimeUnit.MILLISECONDS))
                .build();
        
        return MongoClients.create(settings);
    }
    
    @Bean
    public ReactiveMongoTemplate reactiveMongoTemplate() {
        return new ReactiveMongoTemplate(reactiveMongoClient(), getDatabaseName());
    }
}
