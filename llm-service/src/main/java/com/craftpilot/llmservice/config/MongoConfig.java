package com.craftpilot.llmservice.config;

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
@EnableReactiveMongoRepositories(basePackages = "com.craftpilot.llmservice.repository")
@Slf4j
public class MongoConfig extends AbstractReactiveMongoConfiguration {

    @Value("${spring.data.mongodb.uri}")
    private String mongoUri;

    @Value("${spring.data.mongodb.database}")
    private String databaseName;

    @Value("${spring.data.mongodb.connection-timeout:30000}")
    private int connectTimeout;

    @Value("${spring.data.mongodb.socket-timeout:60000}")
    private int socketTimeout;

    @Override
    protected String getDatabaseName() {
        return databaseName;
    }

    @Override
    @Bean
    public MongoClient reactiveMongoClient() {
        log.info("MongoDB yapılandırılıyor. Veritabanı: {}", databaseName);

        // Hassas bilgileri maskele
        String safeUri = mongoUri.replaceAll("mongodb://[^:]*:[^@]*@", "mongodb://***:***@");
        log.info("MongoDB URI (maskelenmiş): {}", safeUri);

        try {
            ConnectionString connectionString = new ConnectionString(mongoUri);
            String host = connectionString.getHosts().get(0);
            log.info("MongoDB host: {}", host);
        } catch (Exception e) {
            log.warn("MongoDB URI ayrıştırma hatası: {}", e.getMessage());
        }

        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(mongoUri))
                .applyToConnectionPoolSettings(builder -> builder
                        .maxSize(20)
                        .minSize(5)
                        .maxWaitTime(30000, TimeUnit.MILLISECONDS)
                        .maxConnectionIdleTime(300000, TimeUnit.MILLISECONDS)
                        .maxConnectionLifeTime(600000, TimeUnit.MILLISECONDS))
                .applyToSocketSettings(builder -> builder
                        .connectTimeout(connectTimeout, TimeUnit.MILLISECONDS)
                        .readTimeout(socketTimeout, TimeUnit.MILLISECONDS))
                .applyToServerSettings(builder -> builder
                        .heartbeatFrequency(20000, TimeUnit.MILLISECONDS))
                .applyToClusterSettings(builder -> builder
                        .serverSelectionTimeout(30000, TimeUnit.MILLISECONDS))
                .build();

        log.info("MongoDB yapılandırma tamamlandı. Bağlantı zaman aşımı: {}ms, soket zaman aşımı: {}ms",
                connectTimeout, socketTimeout);

        return MongoClients.create(settings);
    }

    @Bean
    public ReactiveMongoTemplate reactiveMongoTemplate() {
        return new ReactiveMongoTemplate(reactiveMongoClient(), getDatabaseName());
    }
}
