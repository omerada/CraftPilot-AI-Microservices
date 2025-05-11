package com.craftpilot.userservice.config;

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
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.ReactiveMongoDatabaseFactory;
import org.springframework.data.mongodb.ReactiveMongoTransactionManager;
import org.springframework.data.mongodb.config.AbstractReactiveMongoConfiguration;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableReactiveMongoRepositories(basePackages = "com.craftpilot.userservice.repository")
@EnableTransactionManagement
@Slf4j
public class MongoConfig extends AbstractReactiveMongoConfiguration {

    @Value("${spring.data.mongodb.uri}")
    private String mongoUri;

    @Value("${spring.data.mongodb.database}")
    private String databaseName;

    @Override
    protected String getDatabaseName() {
        return databaseName;
    }

    @Bean
    @Primary
    @Override
    public MongoClient reactiveMongoClient() {
        log.info("Initializing MongoDB client with URI pattern: {}", 
                mongoUri.replaceAll("mongodb://.*@", "mongodb://***:***@"));
                
        ConnectionString connectionString = new ConnectionString(mongoUri);
        
        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(connectionString)
                .readPreference(ReadPreference.primaryPreferred())
                .writeConcern(WriteConcern.MAJORITY.withWTimeout(2000, TimeUnit.MILLISECONDS))
                .applyToConnectionPoolSettings(builder -> 
                    builder.maxSize(20)
                           .minSize(5)
                           .maxWaitTime(10000, TimeUnit.MILLISECONDS)
                           .maxConnectionLifeTime(1800000, TimeUnit.MILLISECONDS)
                           .maxConnectionIdleTime(600000, TimeUnit.MILLISECONDS)
                )
                .applyToSocketSettings(builder ->
                    builder.connectTimeout(5000, TimeUnit.MILLISECONDS)
                           .readTimeout(10000, TimeUnit.MILLISECONDS)
                )
                .applyToClusterSettings(builder ->
                    builder.serverSelectionTimeout(10000, TimeUnit.MILLISECONDS)
                )
                .build();
                
        return MongoClients.create(settings);
    }
    
    @Bean
    public ReactiveMongoTemplate reactiveMongoTemplate() throws Exception {
        ReactiveMongoTemplate template = new ReactiveMongoTemplate(reactiveMongoClient(), getDatabaseName());
        // Özel dönüşümler için yapılandırma
        MappingMongoConverter converter = (MappingMongoConverter) template.getConverter();
        converter.setMapKeyDotReplacement("_");
        return template;
    }
    
    @Bean
    public ReactiveTransactionManager reactiveTransactionManager(ReactiveMongoDatabaseFactory factory) {
        return new ReactiveMongoTransactionManager(factory);
    }
}
