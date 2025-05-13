package com.craftpilot.activitylogservice.config;

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
@EnableReactiveMongoRepositories(basePackages = "com.craftpilot.activitylogservice.repository")
@Slf4j
public class MongoConfig extends AbstractReactiveMongoConfiguration {

    @Value("${spring.data.mongodb.uri}")
    private String mongoUri;

    @Value("${spring.data.mongodb.database}")
    private String databaseName;

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
        return databaseName;
    }

    @Override
    @Bean
    public MongoClient reactiveMongoClient() {
        log.info("MongoDB yapılandırması başlatılıyor. Database: {}", databaseName);
        
        // Hassas bilgileri gizleyerek URI'yi loglama
        String safeUri = mongoUri.replaceAll("mongodb://[^:]*:[^@]*@", "mongodb://***:***@");
        log.info("MongoDB URI (masked): {}", safeUri);
        
        ConnectionString connectionString = new ConnectionString(mongoUri);
        
        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(connectionString)
                .applyToConnectionPoolSettings(builder -> 
                        builder.maxSize(connectionPoolMaxSize)
                               .maxConnectionIdleTime(maxConnectionIdleTime, TimeUnit.MILLISECONDS))
                .applyToSocketSettings(builder -> 
                        builder.connectTimeout(connectionTimeout, TimeUnit.MILLISECONDS)
                               .readTimeout(socketTimeout, TimeUnit.MILLISECONDS))
                .build();
        
        log.info("MongoDB yapılandırması tamamlandı");
        return MongoClients.create(settings);
    }

    @Bean
    public ReactiveMongoTemplate reactiveMongoTemplate() {
        return new ReactiveMongoTemplate(reactiveMongoClient(), getDatabaseName());
    }
}
