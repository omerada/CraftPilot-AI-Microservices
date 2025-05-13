package com.craftpilot.analyticsservice.config;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class MongoInitializerConfig {

    private final MongoTemplate mongoTemplate;
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
            boolean collectionsCreated = false;
            for (String collectionName : REQUIRED_COLLECTIONS) {
                if (!collectionExists(database, collectionName)) {
                    log.info("Creating collection: {}", collectionName);
                    database.createCollection(collectionName);
                    collectionsCreated = true;
                }
            }
            
            // Only create indexes if collections were newly created or don't have indexes
            if (collectionsCreated || !hasIndexes(database, "usage_metrics")) {
                log.info("Creating indexes for analytics collections...");
                createIndexes(database);
                log.info("MongoDB indexes created successfully");
            } else {
                log.info("MongoDB collections and indexes already exist, skipping initialization");
            }
            
        } catch (Exception e) {
            log.error("Error initializing MongoDB: {}", e.getMessage(), e);
            // Don't fail application startup due to initialization error
            // The application can still function, and indexes can be created manually if needed
        }
    }
    
    private boolean collectionExists(MongoDatabase database, String collectionName) {
        return database.listCollectionNames()
                .into(new java.util.ArrayList<>())
                .contains(collectionName);
    }
    
    private boolean hasIndexes(MongoDatabase database, String collectionName) {
        // Check if collection has more than the default _id index
        return database.getCollection(collectionName)
                .listIndexes()
                .into(new java.util.ArrayList<>())
                .size() > 1;
    }
    
    private void createIndexes(MongoDatabase database) {
        IndexOptions backgroundIndexOption = new IndexOptions().background(true);
        
        // Usage Metrics indexes
        database.getCollection("usage_metrics")
                .createIndex(Indexes.ascending("userId"), backgroundIndexOption);
        database.getCollection("usage_metrics")
                .createIndex(Indexes.ascending("serviceType"), backgroundIndexOption);
        database.getCollection("usage_metrics")
                .createIndex(Indexes.ascending("modelId"), backgroundIndexOption);
        database.getCollection("usage_metrics")
                .createIndex(Indexes.ascending("serviceId"), backgroundIndexOption);
        database.getCollection("usage_metrics")
                .createIndex(Indexes.descending("startTime"), backgroundIndexOption);
        database.getCollection("usage_metrics")
                .createIndex(Indexes.descending("endTime"), backgroundIndexOption);
        database.getCollection("usage_metrics")
                .createIndex(Indexes.descending("createdAt"), backgroundIndexOption);
        
        // Performance Metrics indexes
        database.getCollection("performance_metrics")
                .createIndex(Indexes.ascending("modelId"), backgroundIndexOption);
        database.getCollection("performance_metrics")
                .createIndex(Indexes.ascending("serviceId"), backgroundIndexOption);
        database.getCollection("performance_metrics")
                .createIndex(Indexes.ascending("type"), backgroundIndexOption);
        database.getCollection("performance_metrics")
                .createIndex(Indexes.descending("timestamp"), backgroundIndexOption);
        database.getCollection("performance_metrics")
                .createIndex(Indexes.descending("createdAt"), backgroundIndexOption);
        
        // Analytics Reports indexes
        database.getCollection("analytics_reports")
                .createIndex(Indexes.ascending("type"), backgroundIndexOption);
        database.getCollection("analytics_reports")
                .createIndex(Indexes.ascending("status"), backgroundIndexOption);
        database.getCollection("analytics_reports")
                .createIndex(Indexes.ascending("createdBy"), backgroundIndexOption);
        database.getCollection("analytics_reports")
                .createIndex(Indexes.descending("reportStartTime"), backgroundIndexOption);
        database.getCollection("analytics_reports")
                .createIndex(Indexes.descending("reportEndTime"), backgroundIndexOption);
        database.getCollection("analytics_reports")
                .createIndex(Indexes.descending("createdAt"), backgroundIndexOption);
        database.getCollection("analytics_reports")
                .createIndex(Indexes.ascending("tags"), backgroundIndexOption);
    }
}
