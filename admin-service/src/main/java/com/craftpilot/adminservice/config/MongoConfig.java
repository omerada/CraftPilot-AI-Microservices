package com.craftpilot.adminservice.config;

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
@EnableReactiveMongoRepositories(basePackages = "com.craftpilot.adminservice.repository")
@Slf4j
public class MongoConfig extends AbstractReactiveMongoConfiguration {

    @Value("${spring.data.mongodb.uri}")
    private String mongoUri;

    @Value("${spring.data.mongodb.database}")
    private String databaseName;

    @Value("${spring.data.mongodb.connect-timeout:60000}")
    private int connectTimeout;

    @Value("${spring.data.mongodb.socket-timeout:60000}")
    private int socketTimeout;

    @Value("${spring.data.mongodb.max-wait-time:120000}")
    private int maxWaitTime;

    @Value("${spring.data.mongodb.max-connection-idle-time:300000}")
    private int maxConnectionIdleTime;

    @Value("${spring.data.mongodb.max-connection-life-time:600000}")
    private int maxConnectionLifeTime;

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
                .applyToConnectionPoolSettings(builder -> builder.maxSize(20)
                        .minSize(5)
                        .maxWaitTime(maxWaitTime, TimeUnit.MILLISECONDS)
                        .maxConnectionIdleTime(maxConnectionIdleTime, TimeUnit.MILLISECONDS)
                        .maxConnectionLifeTime(maxConnectionLifeTime, TimeUnit.MILLISECONDS))
                .applyToSocketSettings(builder -> builder.connectTimeout(connectTimeout, TimeUnit.MILLISECONDS)
                        .readTimeout(socketTimeout, TimeUnit.MILLISECONDS))
                .applyToServerSettings(builder -> builder.heartbeatFrequency(20000, TimeUnit.MILLISECONDS))
                .build();

        log.info(
                "MongoDB yapılandırması tamamlandı. Timeout ayarları: connectTimeout={}ms, socketTimeout={}ms, maxWaitTime={}ms",
                connectTimeout, socketTimeout, maxWaitTime);
        return MongoClients.create(settings);
    }

    @Bean
    public ReactiveMongoTemplate reactiveMongoTemplate() {
        return new ReactiveMongoTemplate(reactiveMongoClient(), getDatabaseName());
    }
}
