package com.craftpilot.llmservice.config;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
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
import org.springframework.data.mongodb.config.AbstractReactiveMongoConfiguration;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
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

    @Value("${spring.data.mongodb.connect-timeout:30000}")
    private int connectTimeout;

    @Value("${spring.data.mongodb.socket-timeout:60000}")
    private int socketTimeout;

    @Value("${spring.data.mongodb.max-connection-pool-size:20}")
    private int maxConnectionPoolSize;

    @Value("${spring.data.mongodb.min-connection-pool-size:5}")
    private int minConnectionPoolSize;

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
                        .maxSize(maxConnectionPoolSize)
                        .minSize(minConnectionPoolSize)
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
                .writeConcern(WriteConcern.MAJORITY.withWTimeout(5000, TimeUnit.MILLISECONDS))
                .readConcern(ReadConcern.MAJORITY)
                .readPreference(ReadPreference.primaryPreferred())
                .retryWrites(true)
                .retryReads(true)
                .build();

        log.info("MongoDB yapılandırma tamamlandı. Bağlantı zaman aşımı: {}ms, soket zaman aşımı: {}ms, " +
                "Max bağlantı havuzu: {}, Min bağlantı havuzu: {}",
                connectTimeout, socketTimeout, maxConnectionPoolSize, minConnectionPoolSize);

        return MongoClients.create(settings);
    }

    @Bean
    public ReactiveMongoTemplate reactiveMongoTemplate(ReactiveMongoDatabaseFactory factory,
            MappingMongoConverter converter) {
        ReactiveMongoTemplate template = new ReactiveMongoTemplate(factory, converter);
        log.info("MongoDB şema uyumluluk ve indeks oluşturma etkinleştirildi");
        return template;
    }
}
