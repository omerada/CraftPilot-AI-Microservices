package com.craftpilot.userservice.config;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.ReadConcern;
import com.mongodb.ReadPreference;
import com.mongodb.WriteConcern;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.ReactiveMongoDatabaseFactory;
import org.springframework.data.mongodb.ReactiveMongoTransactionManager;
import org.springframework.data.mongodb.config.AbstractReactiveMongoConfiguration;
import org.springframework.data.mongodb.config.EnableReactiveMongoAuditing;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.util.concurrent.TimeUnit;

@Slf4j
@Configuration
@EnableReactiveMongoRepositories(basePackages = "com.craftpilot.userservice.repository")
@EnableReactiveMongoAuditing
@EnableTransactionManagement
public class MongoConfig extends AbstractReactiveMongoConfiguration {

    @Value("${spring.application.mongodb.uri}")
    private String mongoUri;

    @Value("${spring.application.mongodb.database}")
    private String databaseName;

    @Value("${spring.application.mongodb.server-selection-timeout:30000}")
    private long serverSelectionTimeout;

    @Value("${spring.application.mongodb.connect-timeout:20000}")
    private int connectTimeout;

    @Value("${spring.application.mongodb.socket-timeout:60000}")
    private int socketTimeout;

    @Override
    protected String getDatabaseName() {
        return databaseName;
    }

    @Override
    @Bean
    public MongoClient reactiveMongoClient() {
        ConnectionString connectionString = new ConnectionString(mongoUri);

        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(connectionString)
                .applyToClusterSettings(
                        builder -> builder.serverSelectionTimeout(serverSelectionTimeout, TimeUnit.MILLISECONDS))
                .applyToConnectionPoolSettings(builder -> builder.maxConnectionIdleTime(300000, TimeUnit.MILLISECONDS)
                        .maxSize(20)
                        .minSize(5))
                .applyToSocketSettings(builder -> builder.connectTimeout(connectTimeout, TimeUnit.MILLISECONDS)
                        .readTimeout(socketTimeout, TimeUnit.MILLISECONDS))
                .readPreference(ReadPreference.primaryPreferred())
                .readConcern(ReadConcern.MAJORITY)
                .writeConcern(WriteConcern.MAJORITY)
                .build();

        log.info("Connecting to MongoDB at: {}", maskConnectionString(connectionString.getConnectionString()));
        return MongoClients.create(settings);
    }

    @Bean
    public ReactiveMongoTemplate reactiveMongoTemplate() throws Exception {
        ReactiveMongoTemplate template = new ReactiveMongoTemplate(reactiveMongoClient(), getDatabaseName());
        MappingMongoConverter converter = (MappingMongoConverter) template.getConverter();
        converter.setMapKeyDotReplacement("_");
        return template;
    }

    @Bean
    public ReactiveTransactionManager reactiveTransactionManager(ReactiveMongoDatabaseFactory factory) {
        return new ReactiveMongoTransactionManager(factory);
    }

    // Mask sensitive connection string parts for logging
    private String maskConnectionString(String connectionString) {
        if (connectionString == null)
            return null;
        return connectionString.replaceAll(":[^:]*@", ":***@");
    }
}
