package com.craftpilot.analyticsservice.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexDefinition;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.data.mongodb.core.query.Criteria;

import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;

@Configuration
@Slf4j
public class MongoIndexConfig {

    private final ReactiveMongoTemplate mongoTemplate;

    @Autowired
    public MongoIndexConfig(ReactiveMongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @PostConstruct
    public void initIndexes() {
        log.info("Creating MongoDB indexes");
        
        // UsageMetrics indexleri
        mongoTemplate.indexOps("usage_metrics").ensureIndex(
            new Index().on("userId", Sort.Direction.ASC)).subscribe();
        mongoTemplate.indexOps("usage_metrics").ensureIndex(
            new Index().on("serviceType", Sort.Direction.ASC)).subscribe();
        mongoTemplate.indexOps("usage_metrics").ensureIndex(
            new Index().on("startTime", Sort.Direction.ASC).on("endTime", Sort.Direction.ASC)).subscribe();
        
        // PerformanceMetrics indexleri
        mongoTemplate.indexOps("performance_metrics").ensureIndex(
            new Index().on("modelId", Sort.Direction.ASC)).subscribe();
        mongoTemplate.indexOps("performance_metrics").ensureIndex(
            new Index().on("timestamp", Sort.Direction.DESC)).subscribe();
        
        // AnalyticsReport indexleri
        mongoTemplate.indexOps("analytics_reports").ensureIndex(
            new Index().on("createdBy", Sort.Direction.ASC)).subscribe();
        mongoTemplate.indexOps("analytics_reports").ensureIndex(
            new Index().on("type", Sort.Direction.ASC).on("status", Sort.Direction.ASC)).subscribe();
        mongoTemplate.indexOps("analytics_reports").ensureIndex(
            new Index().on("reportStartTime", Sort.Direction.ASC).on("reportEndTime", Sort.Direction.ASC)).subscribe();
        
        log.info("MongoDB indexes created successfully");
    }
}
