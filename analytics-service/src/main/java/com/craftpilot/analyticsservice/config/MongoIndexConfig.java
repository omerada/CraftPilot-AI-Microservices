package com.craftpilot.analyticsservice.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.domain.Sort;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Configuration
@Slf4j
public class MongoIndexConfig {

    private final ReactiveMongoTemplate mongoTemplate;
    private boolean indexesInitialized = false;  // İndekslerin sadece bir kez oluşturulmasını sağlayan flag

    @Autowired
    public MongoIndexConfig(ReactiveMongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @EventListener(ContextRefreshedEvent.class)
    public void initIndexesAfterStartup() {
        if (indexesInitialized) {
            return;  // İndeksler zaten oluşturulmuşsa tekrar oluşturma
        }
        
        log.info("Creating MongoDB indexes");
        
        try {
            // UsageMetrics indeksleri
            mongoTemplate.indexOps("usage_metrics").ensureIndex(
                new Index().on("userId", Sort.Direction.ASC))
                .onErrorResume(e -> {
                    log.error("Error creating userId index for usage_metrics: {}", e.getMessage());
                    return Mono.empty();
                })
                .block();
                
            mongoTemplate.indexOps("usage_metrics").ensureIndex(
                new Index().on("serviceType", Sort.Direction.ASC))
                .onErrorResume(e -> {
                    log.error("Error creating serviceType index for usage_metrics: {}", e.getMessage());
                    return Mono.empty();
                })
                .block();
                
            mongoTemplate.indexOps("usage_metrics").ensureIndex(
                new Index().on("startTime", Sort.Direction.ASC).on("endTime", Sort.Direction.ASC))
                .onErrorResume(e -> {
                    log.error("Error creating time range index for usage_metrics: {}", e.getMessage());
                    return Mono.empty();
                })
                .block();
            
            // PerformanceMetrics indeksleri
            mongoTemplate.indexOps("performance_metrics").ensureIndex(
                new Index().on("modelId", Sort.Direction.ASC))
                .onErrorResume(e -> {
                    log.error("Error creating modelId index for performance_metrics: {}", e.getMessage());
                    return Mono.empty();
                })
                .block();
                
            mongoTemplate.indexOps("performance_metrics").ensureIndex(
                new Index().on("timestamp", Sort.Direction.DESC))
                .onErrorResume(e -> {
                    log.error("Error creating timestamp index for performance_metrics: {}", e.getMessage());
                    return Mono.empty();
                })
                .block();
            
            // AnalyticsReport indeksleri
            mongoTemplate.indexOps("analytics_reports").ensureIndex(
                new Index().on("createdBy", Sort.Direction.ASC))
                .onErrorResume(e -> {
                    log.error("Error creating createdBy index for analytics_reports: {}", e.getMessage());
                    return Mono.empty();
                })
                .block();
                
            mongoTemplate.indexOps("analytics_reports").ensureIndex(
                new Index().on("type", Sort.Direction.ASC).on("status", Sort.Direction.ASC))
                .onErrorResume(e -> {
                    log.error("Error creating type-status index for analytics_reports: {}", e.getMessage());
                    return Mono.empty();
                })
                .block();
                
            mongoTemplate.indexOps("analytics_reports").ensureIndex(
                new Index().on("reportStartTime", Sort.Direction.ASC).on("reportEndTime", Sort.Direction.ASC))
                .onErrorResume(e -> {
                    log.error("Error creating time range index for analytics_reports: {}", e.getMessage());
                    return Mono.empty();
                })
                .block();
            
            log.info("MongoDB indexes created successfully");
            indexesInitialized = true;
        } catch (Exception e) {
            log.error("Failed to create MongoDB indexes", e);
        }
    }
}
