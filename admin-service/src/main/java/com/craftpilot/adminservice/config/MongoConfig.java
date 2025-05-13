package com.craftpilot.adminservice.config;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractReactiveMongoConfiguration;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableReactiveMongoRepositories(basePackages = "com.craftpilot.adminservice.repository")
public class MongoConfig extends AbstractReactiveMongoConfiguration {
    private static final Logger logger = LoggerFactory.getLogger(MongoConfig.class);

    @Value("${spring.data.mongodb.uri}")
    private String mongoUri;

    @Value("${spring.data.mongodb.database}")
    private String databaseName;

    @Value("${spring.data.mongodb.connection-timeout:10000}")
    private int connectionTimeout;

    @Value("${spring.data.mongodb.max-connection-idle-time:60000}")
    private int maxConnectionIdleTime;

    @Value("${spring.data.mongodb.connection-pool-max-size:50}")
    private int connectionPoolMaxSize;

    @Override
    protected String getDatabaseName() {
        return databaseName;
    }

    @Override
    @Bean
    public MongoClient reactiveMongoClient() {
        try {
            // Hassas bilgileri gizleyerek URI'yi loglama
            String safeUri = mongoUri.replaceAll("mongodb://[^:]*:[^@]*@", "mongodb://***:***@");
            logger.info("Initializing MongoDB configuration. Database: {}, URI: {}", databaseName, safeUri);

            // MongoDB bağlantı URI'sinde server adını çıkar ve logla
            String serverAddress = extractServerAddress(mongoUri);
            logger.info("Attempting to connect to MongoDB server at: {}", serverAddress);

            ConnectionString connectionString = new ConnectionString(mongoUri);

            MongoClientSettings settings = MongoClientSettings.builder()
                    .applyConnectionString(connectionString)
                    .applyToConnectionPoolSettings(builder -> builder
                            .maxSize(connectionPoolMaxSize)
                            .minSize(5)
                            .maxConnectionIdleTime(maxConnectionIdleTime, TimeUnit.MILLISECONDS)
                            .maxWaitTime(15000, TimeUnit.MILLISECONDS))
                    .applyToSocketSettings(builder -> builder
                            .connectTimeout(connectionTimeout, TimeUnit.MILLISECONDS)
                            .readTimeout(30000, TimeUnit.MILLISECONDS))
                    .applyToServerSettings(builder -> builder
                            .heartbeatFrequency(20000, TimeUnit.MILLISECONDS))
                    .retryWrites(true)
                    .retryReads(true)
                    .build();

            logger.info("MongoDB configuration successfully initialized");
            return MongoClients.create(settings);
        } catch (Exception e) {
            logger.error("Failed to configure MongoDB connection", e);
            throw e;
        }
    }

    @Bean
    public ReactiveMongoTemplate reactiveMongoTemplate() {
        try {
            return new ReactiveMongoTemplate(reactiveMongoClient(), getDatabaseName());
        } catch (Exception e) {
            logger.error("Failed to create ReactiveMongoTemplate", e);
            throw e;
        }
    }

    /**
     * MongoDB URI'sinden server adresini çıkarır
     */
    private String extractServerAddress(String uri) {
        try {
            // mongodb://user:pass@server:port/db?options formatını parçala
            int atIndex = uri.indexOf('@');
            int slashAfterServerIndex = -1;

            if (atIndex > 0) {
                // @ işareti varsa ondan sonraki ilk / işaretine kadar al
                slashAfterServerIndex = uri.indexOf('/', atIndex);
                if (slashAfterServerIndex > 0) {
                    return uri.substring(atIndex + 1, slashAfterServerIndex);
                } else {
                    // / işareti bulunamazsa, soru işaretine kadar al
                    int questionIndex = uri.indexOf('?', atIndex);
                    if (questionIndex > 0) {
                        return uri.substring(atIndex + 1, questionIndex);
                    } else {
                        // soru işareti de bulunamazsa tümünü al
                        return uri.substring(atIndex + 1);
                    }
                }
            } else {
                // @ işareti yoksa mongodb:// sonrası ilk / işaretine kadar al
                int protocolIndex = uri.indexOf("://");
                if (protocolIndex > 0) {
                    slashAfterServerIndex = uri.indexOf('/', protocolIndex + 3);
                    if (slashAfterServerIndex > 0) {
                        return uri.substring(protocolIndex + 3, slashAfterServerIndex);
                    } else {
                        int questionIndex = uri.indexOf('?', protocolIndex);
                        if (questionIndex > 0) {
                            return uri.substring(protocolIndex + 3, questionIndex);
                        } else {
                            return uri.substring(protocolIndex + 3);
                        }
                    }
                }
            }
            return "unknown-server";
        } catch (Exception e) {
            logger.warn("Failed to extract server address from MongoDB URI", e);
            return "parsing-error";
        }
    }
}
