package com.craftpilot.analyticsservice.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class MongoIndexConfig {

    private final ReactiveMongoTemplate mongoTemplate;
    
    @Value("${mongodb.indexes.creation.enabled:true}")
    private boolean indexCreationEnabled;
    
    @EventListener(ContextRefreshedEvent.class)
    public void initIndices() {
        if (!indexCreationEnabled) {
            log.info("MongoDB index creation is disabled");
            return;
        }
        
        log.info("Initializing MongoDB indexes");
        
        // Usage Metrics indeksleri
        createIndex("usage_metrics", new Index().on("userId", Sort.Direction.ASC))
            .and(createIndex("usage_metrics", new Index().on("serviceType", Sort.Direction.ASC)))
            .and(createIndex("usage_metrics", new Index().on("startTime", Sort.Direction.DESC)))
            .and(createIndex("usage_metrics", new Index().on("endTime", Sort.Direction.DESC)))
            .and(createIndex("usage_metrics", new Index().on("createdAt", Sort.Direction.DESC)))
            .block(Duration.ofMillis(TimeUnit.SECONDS.toMillis(30)));
        
        // Performance Metrics indeksleri
        createIndex("performance_metrics", new Index().on("modelId", Sort.Direction.ASC))
            .and(createIndex("performance_metrics", new Index().on("serviceId", Sort.Direction.ASC)))
            .and(createIndex("performance_metrics", new Index().on("type", Sort.Direction.ASC)))
            .and(createIndex("performance_metrics", new Index().on("timestamp", Sort.Direction.DESC)))
            .block(Duration.ofMillis(TimeUnit.SECONDS.toMillis(30)));
        
        // Analytics Reports indeksleri
        createIndex("analytics_reports", new Index().on("type", Sort.Direction.ASC))
            .and(createIndex("analytics_reports", new Index().on("status", Sort.Direction.ASC)))
            .and(createIndex("analytics_reports", new Index().on("createdBy", Sort.Direction.ASC)))
            .and(createIndex("analytics_reports", new Index().on("reportStartTime", Sort.Direction.DESC)))
            .and(createIndex("analytics_reports", new Index().on("reportEndTime", Sort.Direction.DESC)))
            .and(createIndex("analytics_reports", new Index().on("createdAt", Sort.Direction.DESC)))
            .block(Duration.ofMillis(TimeUnit.SECONDS.toMillis(30)));
        
        log.info("MongoDB indexes initialized successfully");
    }
    
    private Mono<String> createIndex(String collection, Index index) {
        return mongoTemplate.indexOps(collection).ensureIndex(index)
            .doOnSuccess(name -> log.info("Created index {} on collection {}", name, collection))
            .doOnError(e -> log.error("Failed to create index on collection {}: {}", collection, e.getMessage()));
    }
}
