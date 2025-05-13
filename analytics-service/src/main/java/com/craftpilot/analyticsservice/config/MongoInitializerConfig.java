package com.craftpilot.analyticsservice.config;

import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoDatabase;
import com.mongodb.reactivestreams.client.MongoCollection;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

@Slf4j
@Component
@RequiredArgsConstructor
public class MongoInitializerConfig {

    private final ReactiveMongoTemplate mongoTemplate;
    private final MongoClient mongoClient;
    
    private static final String DATABASE_NAME = "analytics";
    private static final List<String> REQUIRED_COLLECTIONS = Arrays.asList(
            "usage_metrics", "performance_metrics", "analytics_reports");
    
    @EventListener(ApplicationReadyEvent.class)
    public void initializeMongoDB() {
        log.info("Checking MongoDB collections and indexes for Analytics Service...");
        
        try {
            MongoDatabase database = mongoClient.getDatabase(DATABASE_NAME);
            
            // Check if collections exist and create them if needed
            Flux.fromIterable(REQUIRED_COLLECTIONS)
                .flatMap(collectionName -> collectionExists(database, collectionName)
                    .flatMap(exists -> {
                        if (!exists) {
                            log.info("Creating collection: {}", collectionName);
                            return createCollection(database, collectionName).thenReturn(true);
                        }
                        return Mono.just(false);
                    })
                )
                .collectList()
                .flatMap(createdList -> {
                    boolean collectionsCreated = createdList.stream().anyMatch(created -> created);
                    
                    return hasIndexes(database, "usage_metrics")
                        .flatMap(hasExistingIndexes -> {
                            if (collectionsCreated || !hasExistingIndexes) {
                                log.info("Creating indexes for analytics collections...");
                                return createIndexes(database).thenReturn(true);
                            } else {
                                log.info("MongoDB collections and indexes already exist, skipping initialization");
                                return Mono.just(false);
                            }
                        });
                })
                .subscribe(
                    result -> {
                        if (result) {
                            log.info("MongoDB indexes created successfully");
                        }
                    },
                    error -> log.error("Error initializing MongoDB: {}", error.getMessage(), error),
                    () -> log.debug("MongoDB initialization process completed")
                );
            
        } catch (Exception e) {
            log.error("Error initializing MongoDB: {}", e.getMessage(), e);
            // Don't fail application startup due to initialization error
            // The application can still function, and indexes can be created manually if needed
        }
    }
    
    private Mono<Boolean> collectionExists(MongoDatabase database, String collectionName) {
        return Flux.from(database.listCollectionNames())
                .collectList()
                .map(collections -> collections.contains(collectionName));
    }
    
    private Mono<Void> createCollection(MongoDatabase database, String collectionName) {
        return Mono.from(database.createCollection(collectionName)).then();
    }
    
    private Mono<Boolean> hasIndexes(MongoDatabase database, String collectionName) {
        return Flux.from(database.getCollection(collectionName).listIndexes())
                .collectList()
                .map(indexes -> indexes.size() > 1);
    }
    
    private Mono<Void> createIndexes(MongoDatabase database) {
        IndexOptions backgroundIndexOption = new IndexOptions().background(true);
        
        List<Mono<Void>> indexCreationOperations = new ArrayList<>();
        
        // Usage Metrics indexes
        MongoCollection<Document> usageMetricsCollection = database.getCollection("usage_metrics");
        indexCreationOperations.add(Mono.from(usageMetricsCollection.createIndex(Indexes.ascending("userId"), backgroundIndexOption)).then());
        indexCreationOperations.add(Mono.from(usageMetricsCollection.createIndex(Indexes.ascending("serviceType"), backgroundIndexOption)).then());
        indexCreationOperations.add(Mono.from(usageMetricsCollection.createIndex(Indexes.ascending("modelId"), backgroundIndexOption)).then());
        indexCreationOperations.add(Mono.from(usageMetricsCollection.createIndex(Indexes.ascending("serviceId"), backgroundIndexOption)).then());
        indexCreationOperations.add(Mono.from(usageMetricsCollection.createIndex(Indexes.descending("startTime"), backgroundIndexOption)).then());
        indexCreationOperations.add(Mono.from(usageMetricsCollection.createIndex(Indexes.descending("endTime"), backgroundIndexOption)).then());
        indexCreationOperations.add(Mono.from(usageMetricsCollection.createIndex(Indexes.descending("createdAt"), backgroundIndexOption)).then());
        
        // Performance Metrics indexes
        MongoCollection<Document> performanceMetricsCollection = database.getCollection("performance_metrics");
        indexCreationOperations.add(Mono.from(performanceMetricsCollection.createIndex(Indexes.ascending("modelId"), backgroundIndexOption)).then());
        indexCreationOperations.add(Mono.from(performanceMetricsCollection.createIndex(Indexes.ascending("serviceId"), backgroundIndexOption)).then());
        indexCreationOperations.add(Mono.from(performanceMetricsCollection.createIndex(Indexes.ascending("type"), backgroundIndexOption)).then());
        indexCreationOperations.add(Mono.from(performanceMetricsCollection.createIndex(Indexes.descending("timestamp"), backgroundIndexOption)).then());
        indexCreationOperations.add(Mono.from(performanceMetricsCollection.createIndex(Indexes.descending("createdAt"), backgroundIndexOption)).then());
        
        // Analytics Reports indexes
        MongoCollection<Document> analyticsReportsCollection = database.getCollection("analytics_reports");
        indexCreationOperations.add(Mono.from(analyticsReportsCollection.createIndex(Indexes.ascending("type"), backgroundIndexOption)).then());
        indexCreationOperations.add(Mono.from(analyticsReportsCollection.createIndex(Indexes.ascending("status"), backgroundIndexOption)).then());
        indexCreationOperations.add(Mono.from(analyticsReportsCollection.createIndex(Indexes.ascending("createdBy"), backgroundIndexOption)).then());
        indexCreationOperations.add(Mono.from(analyticsReportsCollection.createIndex(Indexes.descending("reportStartTime"), backgroundIndexOption)).then());
        indexCreationOperations.add(Mono.from(analyticsReportsCollection.createIndex(Indexes.descending("reportEndTime"), backgroundIndexOption)).then());
        indexCreationOperations.add(Mono.from(analyticsReportsCollection.createIndex(Indexes.descending("createdAt"), backgroundIndexOption)).then());
        indexCreationOperations.add(Mono.from(analyticsReportsCollection.createIndex(Indexes.ascending("tags"), backgroundIndexOption)).then());
        
        return Flux.concat(indexCreationOperations).then();
    }
}
