package com.craftpilot.analyticsservice.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
@ConditionalOnProperty(name = "spring.data.mongodb.auto-index-creation", havingValue = "true", matchIfMissing = false)
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
        
        try {
            // Usage Metrics indeksleri
            createIndex("usage_metrics", new Index().on("userId", Sort.Direction.ASC))
                .doOnSuccess(name -> log.info("Created index {} on collection {}", name, "usage_metrics"))
                .doOnError(e -> log.error("Failed to create index on collection {}: {}", "usage_metrics", e.getMessage()))
                .onErrorResume(e -> Mono.empty())
                .subscribe();
            
            createIndex("usage_metrics", new Index().on("serviceType", Sort.Direction.ASC))
                .doOnSuccess(name -> log.info("Created index {} on collection {}", name, "usage_metrics"))
                .doOnError(e -> log.error("Failed to create index on collection {}: {}", "usage_metrics", e.getMessage()))
                .onErrorResume(e -> Mono.empty())
                .subscribe();
            
            // Performance Metrics indeksleri
            createIndex("performance_metrics", new Index().on("modelId", Sort.Direction.ASC))
                .doOnSuccess(name -> log.info("Created index {} on collection {}", name, "performance_metrics"))
                .doOnError(e -> log.error("Failed to create index on collection {}: {}", "performance_metrics", e.getMessage()))
                .onErrorResume(e -> Mono.empty())
                .subscribe();
            
            createIndex("performance_metrics", new Index().on("type", Sort.Direction.ASC))
                .doOnSuccess(name -> log.info("Created index {} on collection {}", name, "performance_metrics"))
                .doOnError(e -> log.error("Failed to create index on collection {}: {}", "performance_metrics", e.getMessage()))
                .onErrorResume(e -> Mono.empty())
                .subscribe();
            
            // Analytics Reports indeksleri
            createIndex("analytics_reports", new Index().on("type", Sort.Direction.ASC))
                .doOnSuccess(name -> log.info("Created index {} on collection {}", name, "analytics_reports"))
                .doOnError(e -> log.error("Failed to create index on collection {}: {}", "analytics_reports", e.getMessage()))
                .onErrorResume(e -> Mono.empty())
                .subscribe();
            
            createIndex("analytics_reports", new Index().on("status", Sort.Direction.ASC))
                .doOnSuccess(name -> log.info("Created index {} on collection {}", name, "analytics_reports"))
                .doOnError(e -> log.error("Failed to create index on collection {}: {}", "analytics_reports", e.getMessage()))
                .onErrorResume(e -> Mono.empty())
                .subscribe();
            
            log.info("MongoDB indexes initialization process started");
        } catch (Exception e) {
            log.error("Error during MongoDB index initialization: {}", e.getMessage(), e);
        }
    }
    
    private Mono<String> createIndex(String collection, Index index) {
        return mongoTemplate.indexOps(collection).ensureIndex(index);
    }
}
