package com.craftpilot.adminservice.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class MongoIndexConfig {

    private final ReactiveMongoTemplate mongoTemplate;

    @EventListener(ContextRefreshedEvent.class)
    public void initIndexes() {
        log.info("Initializing MongoDB indexes for admin-service collections");

        // UserActivity collection indexes
        mongoTemplate.indexOps("user_activities")
                .ensureIndex(new Index().on("userId", Sort.Direction.ASC))
                .subscribe(result -> log.info("UserActivity userId index created: {}", result));

        mongoTemplate.indexOps("user_activities")
                .ensureIndex(new Index().on("activityType", Sort.Direction.ASC))
                .subscribe(result -> log.info("UserActivity activityType index created: {}", result));

        mongoTemplate.indexOps("user_activities")
                .ensureIndex(new Index().on("timestamp", Sort.Direction.DESC))
                .subscribe(result -> log.info("UserActivity timestamp index created: {}", result));

        mongoTemplate.indexOps("user_activities")
                .ensureIndex(new Index().on("status", Sort.Direction.ASC))
                .subscribe(result -> log.info("UserActivity status index created: {}", result));

        // SystemMetrics collection indexes
        mongoTemplate.indexOps("system_metrics")
                .ensureIndex(new Index().on("serviceId", Sort.Direction.ASC).unique())
                .subscribe(result -> log.info("SystemMetrics serviceId index created: {}", result));

        mongoTemplate.indexOps("system_metrics")
                .ensureIndex(new Index().on("serviceType", Sort.Direction.ASC))
                .subscribe(result -> log.info("SystemMetrics serviceType index created: {}", result));

        mongoTemplate.indexOps("system_metrics")
                .ensureIndex(new Index().on("timestamp", Sort.Direction.DESC))
                .subscribe(result -> log.info("SystemMetrics timestamp index created: {}", result));

        mongoTemplate.indexOps("system_metrics")
                .ensureIndex(new Index().on("status", Sort.Direction.ASC))
                .subscribe(result -> log.info("SystemMetrics status index created: {}", result));

        // SystemAlert collection indexes
        mongoTemplate.indexOps("system_alerts")
                .ensureIndex(new Index().on("serviceId", Sort.Direction.ASC))
                .subscribe(result -> log.info("SystemAlert serviceId index created: {}", result));

        mongoTemplate.indexOps("system_alerts")
                .ensureIndex(new Index().on("alertType", Sort.Direction.ASC))
                .subscribe(result -> log.info("SystemAlert alertType index created: {}", result));

        mongoTemplate.indexOps("system_alerts")
                .ensureIndex(new Index().on("severity", Sort.Direction.ASC))
                .subscribe(result -> log.info("SystemAlert severity index created: {}", result));

        mongoTemplate.indexOps("system_alerts")
                .ensureIndex(new Index().on("timestamp", Sort.Direction.DESC))
                .subscribe(result -> log.info("SystemAlert timestamp index created: {}", result));

        mongoTemplate.indexOps("system_alerts")
                .ensureIndex(new Index().on("status", Sort.Direction.ASC))
                .subscribe(result -> log.info("SystemAlert status index created: {}", result));

        // AuditLog collection indexes
        mongoTemplate.indexOps("audit_logs")
                .ensureIndex(new Index().on("userId", Sort.Direction.ASC))
                .subscribe(result -> log.info("AuditLog userId index created: {}", result));

        mongoTemplate.indexOps("audit_logs")
                .ensureIndex(new Index().on("serviceId", Sort.Direction.ASC))
                .subscribe(result -> log.info("AuditLog serviceId index created: {}", result));

        mongoTemplate.indexOps("audit_logs")
                .ensureIndex(new Index().on("logType", Sort.Direction.ASC))
                .subscribe(result -> log.info("AuditLog logType index created: {}", result));

        mongoTemplate.indexOps("audit_logs")
                .ensureIndex(new Index().on("timestamp", Sort.Direction.DESC))
                .subscribe(result -> log.info("AuditLog timestamp index created: {}", result));

        // AdminAction collection indexes
        mongoTemplate.indexOps("admin_actions")
                .ensureIndex(new Index().on("adminId", Sort.Direction.ASC))
                .subscribe(result -> log.info("AdminAction adminId index created: {}", result));

        mongoTemplate.indexOps("admin_actions")
                .ensureIndex(new Index().on("actionType", Sort.Direction.ASC))
                .subscribe(result -> log.info("AdminAction actionType index created: {}", result));

        mongoTemplate.indexOps("admin_actions")
                .ensureIndex(new Index().on("targetId", Sort.Direction.ASC))
                .subscribe(result -> log.info("AdminAction targetId index created: {}", result));

        mongoTemplate.indexOps("admin_actions")
                .ensureIndex(new Index().on("timestamp", Sort.Direction.DESC))
                .subscribe(result -> log.info("AdminAction timestamp index created: {}", result));

        log.info("MongoDB indexes initialization completed");
    }
}
