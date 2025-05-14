package com.craftpilot.notificationservice.config;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.ReactiveMongoDatabaseFactory;
import org.springframework.data.mongodb.config.AbstractReactiveMongoConfiguration;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableReactiveMongoRepositories(basePackages = "com.craftpilot.notificationservice.repository")
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
    public MongoClient reactiveMongoClient() {
        ConnectionString connectionString = new ConnectionString(mongoUri);
        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(connectionString)
                .applyToConnectionPoolSettings(builder -> builder.maxSize(100)
                        .minSize(5)
                        .maxWaitTime(2, TimeUnit.MINUTES)
                        .maxConnectionLifeTime(1, TimeUnit.HOURS)
                        .maxConnectionIdleTime(10, TimeUnit.MINUTES))
                .applyToSocketSettings(builder -> builder.connectTimeout(5, TimeUnit.SECONDS)
                        .readTimeout(2, TimeUnit.MINUTES))
                .build();

        return MongoClients.create(settings);
    }

    @Bean
    public ReactiveMongoTemplate reactiveMongoTemplate() {
        return new ReactiveMongoTemplate(reactiveMongoClient(), getDatabaseName());
    }

    @Override
    public MappingMongoConverter mappingMongoConverter(ReactiveMongoDatabaseFactory databaseFactory,
            MongoCustomConversions customConversions,
            MongoMappingContext mappingContext) {
        MappingMongoConverter converter = super.mappingMongoConverter(databaseFactory, customConversions,
                mappingContext);
        converter.setMapKeyDotReplacement("_");
        return converter;
    }
}
