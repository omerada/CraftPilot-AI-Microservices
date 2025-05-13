package com.craftpilot.analyticsservice.config;

import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoDatabase;
import com.mongodb.reactivestreams.client.MongoCollection;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
@RequiredArgsConstructor
public class MongoInitializerConfig {

    private final ReactiveMongoTemplate mongoTemplate;
    private final MongoClient mongoClient;
    
    @Value("${spring.data.mongodb.database:craftpilot}")
    private String DATABASE_NAME;
    
    private static final List<String> REQUIRED_COLLECTIONS = Arrays.asList(
            "usage_metrics", "performance_metrics", "analytics_reports");
    
    private AtomicBoolean initializationComplete = new AtomicBoolean(false);
    
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
                            return createCollection(database, collectionName)
                                .doOnError(e -> log.warn("Could not create collection {}: {}", collectionName, e.getMessage()))
                                .onErrorResume(e -> Mono.empty())
                                .then(Mono.just(true));  
                        }
                        return Mono.just(false);
                    })
                    .retryWhen(Retry.backoff(3, Duration.ofSeconds(2))
                        .doAfterRetry(rs -> log.warn("Retrying collection check for {} after failure", collectionName))
                        .onRetryExhaustedThrow((spec, rs) -> rs.failure()))
                    .onErrorResume(e -> {
                        log.warn("Error checking collection {}: {}", collectionName, e.getMessage());
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
                                return createIndexes(database)
                                    .doOnError(e -> log.warn("Could not create indexes: {}", e.getMessage()))
                                    .onErrorResume(e -> Mono.empty())
                                    .thenReturn(true);
                            } else {
                                log.info("MongoDB collections and indexes already exist, skipping initialization");
                                return Mono.just(false);
                            }
                        })
                        .onErrorResume(e -> {
                            log.warn("Error checking indexes: {}", e.getMessage());
                            return Mono.just(false);
                        });
                })
                .doOnSuccess(result -> {
                    initializationComplete.set(true);
                    if (result) {
                        log.info("MongoDB indexes created successfully");
                    }
                })
                .doOnError(error -> {
                    log.error("Error initializing MongoDB: {}", error.getMessage(), error);
                    // Başarısız olsa bile initialization tamamlandı olarak işaretliyoruz
                    initializationComplete.set(true);
                })
                .subscribe();
            
        } catch (Exception e) {
            log.error("Error initializing MongoDB: {}", e.getMessage(), e);
            // Don't fail application startup due to initialization error
            // The application can still function, and indexes can be created manually if needed
            initializationComplete.set(true);
        }
    }
    
    private Mono<Boolean> collectionExists(MongoDatabase database, String collectionName) {
        return Flux.from(database.listCollectionNames())
                .collectList()
                .map(collections -> collections.contains(collectionName))
                .onErrorResume(e -> {
                    log.warn("Error checking if collection {} exists: {}", collectionName, e.getMessage());
                    return Mono.just(false);
                });
    }
    
    private Mono<Void> createCollection(MongoDatabase database, String collectionName) {
        return Mono.from(database.createCollection(collectionName))
                .onErrorResume(e -> {
                    log.warn("Error creating collection {}: {}", collectionName, e.getMessage());
                    return Mono.empty();
                })
                .then();
    }
    
    private Mono<Boolean> hasIndexes(MongoDatabase database, String collectionName) {
        return Flux.from(database.getCollection(collectionName).listIndexes())
                .collectList()
                .map(indexes -> indexes.size() > 1)
                .onErrorResume(e -> {
                    log.warn("Error checking indexes for collection {}: {}", collectionName, e.getMessage());
                    return Mono.just(false);
                });
    }
    
    private Mono<Void> createIndexes(MongoDatabase database) {
        IndexOptions backgroundIndexOption = new IndexOptions().background(true);
        
        List<Mono<Void>> indexCreationOperations = new ArrayList<>();
        
        try {
            // Usage Metrics indexes
            MongoCollection<Document> usageMetricsCollection = database.getCollection("usage_metrics");
            indexCreationOperations.add(Mono.from(usageMetricsCollection.createIndex(Indexes.ascending("userId"), backgroundIndexOption))
                    .onErrorResume(e -> {
                        log.warn("Error creating userId index on usage_metrics: {}", e.getMessage());
                        return Mono.empty();
                    }).then());
            indexCreationOperations.add(Mono.from(usageMetricsCollection.createIndex(Indexes.ascending("serviceType"), backgroundIndexOption))
                    .onErrorResume(e -> {
                        log.warn("Error creating serviceType index on usage_metrics: {}", e.getMessage());
                        return Mono.empty();
                    }).then());
            indexCreationOperations.add(Mono.from(usageMetricsCollection.createIndex(Indexes.ascending("modelId"), backgroundIndexOption))
                    .onErrorResume(e -> {
                        log.warn("Error creating modelId index on usage_metrics: {}", e.getMessage());
                        return Mono.empty();
                    }).then());
            indexCreationOperations.add(Mono.from(usageMetricsCollection.createIndex(Indexes.ascending("serviceId"), backgroundIndexOption))
                    .onErrorResume(e -> {
                        log.warn("Error creating serviceId index on usage_metrics: {}", e.getMessage());
                        return Mono.empty();
                    }).then());
            indexCreationOperations.add(Mono.from(usageMetricsCollection.createIndex(Indexes.descending("startTime"), backgroundIndexOption))
                    .onErrorResume(e -> {
                        log.warn("Error creating startTime index on usage_metrics: {}", e.getMessage());
                        return Mono.empty();
                    }).then());
            indexCreationOperations.add(Mono.from(usageMetricsCollection.createIndex(Indexes.descending("endTime"), backgroundIndexOption))
                    .onErrorResume(e -> {
                        log.warn("Error creating endTime index on usage_metrics: {}", e.getMessage());
                        return Mono.empty();
                    }).then());
            indexCreationOperations.add(Mono.from(usageMetricsCollection.createIndex(Indexes.descending("createdAt"), backgroundIndexOption))
                    .onErrorResume(e -> {
                        log.warn("Error creating createdAt index on usage_metrics: {}", e.getMessage());
                        return Mono.empty();
                    }).then());
            
            // Performance Metrics indexes
            MongoCollection<Document> performanceMetricsCollection = database.getCollection("performance_metrics");
            indexCreationOperations.add(Mono.from(performanceMetricsCollection.createIndex(Indexes.ascending("modelId"), backgroundIndexOption))
                    .onErrorResume(e -> {
                        log.warn("Error creating modelId index on performance_metrics: {}", e.getMessage());
                        return Mono.empty();
                    }).then());
            indexCreationOperations.add(Mono.from(performanceMetricsCollection.createIndex(Indexes.ascending("serviceId"), backgroundIndexOption))
                    .onErrorResume(e -> {
                        log.warn("Error creating serviceId index on performance_metrics: {}", e.getMessage());
                        return Mono.empty();
                    }).then());
            indexCreationOperations.add(Mono.from(performanceMetricsCollection.createIndex(Indexes.ascending("type"), backgroundIndexOption))
                    .onErrorResume(e -> {
                        log.warn("Error creating type index on performance_metrics: {}", e.getMessage());
                        return Mono.empty();
                    }).then());
            indexCreationOperations.add(Mono.from(performanceMetricsCollection.createIndex(Indexes.descending("timestamp"), backgroundIndexOption))
                    .onErrorResume(e -> {
                        log.warn("Error creating timestamp index on performance_metrics: {}", e.getMessage());
                        return Mono.empty();
                    }).then());
            indexCreationOperations.add(Mono.from(performanceMetricsCollection.createIndex(Indexes.descending("createdAt"), backgroundIndexOption))
                    .onErrorResume(e -> {
                        log.warn("Error creating createdAt index on performance_metrics: {}", e.getMessage());
                        return Mono.empty();
                    }).then());
            
            // Analytics Reports indexes
            MongoCollection<Document> analyticsReportsCollection = database.getCollection("analytics_reports");
            indexCreationOperations.add(Mono.from(analyticsReportsCollection.createIndex(Indexes.ascending("type"), backgroundIndexOption))
                    .onErrorResume(e -> {
                        log.warn("Error creating type index on analytics_reports: {}", e.getMessage());
                        return Mono.empty();
                    }).then());
            indexCreationOperations.add(Mono.from(analyticsReportsCollection.createIndex(Indexes.ascending("status"), backgroundIndexOption))
                    .onErrorResume(e -> {
                        log.warn("Error creating status index on analytics_reports: {}", e.getMessage());
                        return Mono.empty();
                    }).then());
            indexCreationOperations.add(Mono.from(analyticsReportsCollection.createIndex(Indexes.ascending("createdBy"), backgroundIndexOption))
                    .onErrorResume(e -> {
                        log.warn("Error creating createdBy index on analytics_reports: {}", e.getMessage());
                        return Mono.empty();
                    }).then());
            indexCreationOperations.add(Mono.from(analyticsReportsCollection.createIndex(Indexes.descending("reportStartTime"), backgroundIndexOption))
                    .onErrorResume(e -> {
                        log.warn("Error creating reportStartTime index on analytics_reports: {}", e.getMessage());
                        return Mono.empty();
                    }).then());
            indexCreationOperations.add(Mono.from(analyticsReportsCollection.createIndex(Indexes.descending("reportEndTime"), backgroundIndexOption))
                    .onErrorResume(e -> {
                        log.warn("Error creating reportEndTime index on analytics_reports: {}", e.getMessage());
                        return Mono.empty();
                    }).then());
            indexCreationOperations.add(Mono.from(analyticsReportsCollection.createIndex(Indexes.descending("createdAt"), backgroundIndexOption))
                    .onErrorResume(e -> {
                        log.warn("Error creating createdAt index on analytics_reports: {}", e.getMessage());
                        return Mono.empty();
                    }).then());
            indexCreationOperations.add(Mono.from(analyticsReportsCollection.createIndex(Indexes.ascending("tags"), backgroundIndexOption))
                    .onErrorResume(e -> {
                        log.warn("Error creating tags index on analytics_reports: {}", e.getMessage());
                        return Mono.empty();
                    }).then());
            
            return Flux.concat(indexCreationOperations)
                    .then()
                    .onErrorResume(e -> {
                        log.warn("Error creating some indexes: {}", e.getMessage());
                        return Mono.empty();
                    });
        } catch (Exception e) {
            log.error("Error preparing index creation operations: {}", e.getMessage(), e);
            return Mono.empty();
        }
    }
}
