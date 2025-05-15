package com.craftpilot.userservice.config;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractReactiveMongoConfiguration;
import org.springframework.data.mongodb.config.EnableReactiveMongoAuditing;
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.util.concurrent.TimeUnit;

@Slf4j
@Configuration
@EnableReactiveMongoRepositories(basePackages = "com.craftpilot.userservice.repository")
@EnableReactiveMongoAuditing
@EnableTransactionManagement
public class MongoConfig extends AbstractReactiveMongoConfiguration {

    @Value("${spring.data.mongodb.uri}")
    private String mongoUri;

    @Value("${spring.data.mongodb.database}")
    private String databaseName;

    @Override
    protected String getDatabaseName() {
        return databaseName;
    }

    @Override
    @Bean
    public MongoClient reactiveMongoClient() {
        try {
            if (!mongoUri.startsWith("mongodb://") && !mongoUri.startsWith("mongodb+srv://")) {
                throw new IllegalArgumentException("MongoDB URI must start with 'mongodb://' or 'mongodb+srv://'");
            }

            ConnectionString connectionString = new ConnectionString(mongoUri);
            MongoClientSettings settings = MongoClientSettings.builder()
                    .applyConnectionString(connectionString)
                    .applyToSocketSettings(builder -> builder.connectTimeout(5000, TimeUnit.MILLISECONDS))
                    .applyToServerSettings(builder -> builder.heartbeatFrequency(10000, TimeUnit.MILLISECONDS))
                    .build();

            log.info("Attempting to connect to MongoDB at: {}", maskConnectionString(mongoUri));
            return MongoClients.create(settings);
        } catch (Exception e) {
            log.error("Failed to create MongoDB client: {}", e.getMessage(), e);
            throw new RuntimeException("Could not create MongoDB client", e);
        }
    }

    private String maskConnectionString(String connectionString) {
        // Daha güvenli maskeleme
        if (connectionString == null)
            return null;
        return connectionString.replaceAll(":[^/@]+@", ":****@");
    }
}
