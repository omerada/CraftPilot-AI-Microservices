package com.craftpilot.adminservice.config;

import com.craftpilot.adminservice.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.ReactiveIndexOperations;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.util.concurrent.TimeUnit;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class MongoIndexConfig {

    private final ReactiveMongoTemplate mongoTemplate;

    @Value("${spring.data.mongodb.auto-index-creation:false}")
    private boolean autoIndexCreation;
    
    @Value("${admin.mongodb.manual-index-creation:false}")
    private boolean manualIndexCreation;

    @PostConstruct
    public void initializeDatabase() {
        if (manualIndexCreation) {
            log.info("MongoDB manual index creation is enabled");
        } else {
            log.debug("MongoDB manual index creation is disabled. Set admin.mongodb.manual-index-creation=true to enable");
        }
    }

    @EventListener(ContextRefreshedEvent.class)
    public void initIndices() {
        if (!manualIndexCreation) {
            // Skip index creation entirely when disabled
            log.debug("Skipping MongoDB index creation as it is disabled");
            return;
        }

        log.info("Initializing MongoDB indexes for admin-service collections");

        createUserActivityIndexes()
                .then(createSystemMetricsIndexes())
                .then(createSystemAlertIndexes())
                .then(createAuditLogIndexes())
                .then(createAdminActionIndexes())
                .doOnSuccess(v -> log.info("MongoDB indexes initialization completed"))
                .doOnError(e -> log.error("Error creating MongoDB indexes: {}", e.getMessage()))
                .subscribe();
    }

    private Mono<Void> createUserActivityIndexes() {
        log.info("UserActivity indexes are being created");
        ReactiveIndexOperations indexOps = mongoTemplate.indexOps(UserActivity.class);

        return Mono.when(
                indexOps.ensureIndex(new Index().on("userId", Sort.Direction.ASC)),
                indexOps.ensureIndex(new Index().on("serviceId", Sort.Direction.ASC)),
                indexOps.ensureIndex(new Index().on("activityType", Sort.Direction.ASC)),
                indexOps.ensureIndex(new Index().on("status", Sort.Direction.ASC)),
                indexOps.ensureIndex(new Index().on("timestamp", Sort.Direction.DESC))
                        .doOnSuccess(name -> log.debug("UserActivity timestamp index created: {}", name))
                        .doOnError(e -> log.error("Error creating UserActivity timestamp index: {}", e.getMessage()))
        ).then();
    }

    private Mono<Void> createSystemMetricsIndexes() {
        log.info("SystemMetrics indexes are being created");
        ReactiveIndexOperations indexOps = mongoTemplate.indexOps(SystemMetrics.class);

        return Mono.when(
                indexOps.ensureIndex(new Index().on("serviceId", Sort.Direction.ASC).unique()),
                indexOps.ensureIndex(new Index().on("serviceType", Sort.Direction.ASC)),
                indexOps.ensureIndex(new Index().on("status", Sort.Direction.ASC)),
                indexOps.ensureIndex(new Index().on("timestamp", Sort.Direction.DESC))
        ).then();
    }

    private Mono<Void> createSystemAlertIndexes() {
        log.info("SystemAlert indexes are being created");
        ReactiveIndexOperations indexOps = mongoTemplate.indexOps(SystemAlert.class);

        return Mono.when(
                indexOps.ensureIndex(new Index().on("serviceId", Sort.Direction.ASC)),
                indexOps.ensureIndex(new Index().on("alertType", Sort.Direction.ASC)),
                indexOps.ensureIndex(new Index().on("severity", Sort.Direction.DESC)),
                indexOps.ensureIndex(new Index().on("status", Sort.Direction.ASC)),
                indexOps.ensureIndex(new Index().on("assignedTo", Sort.Direction.ASC)),
                indexOps.ensureIndex(new Index().on("timestamp", Sort.Direction.DESC)),
                indexOps.ensureIndex(new Index().on("createdAt", Sort.Direction.DESC).expire(90, TimeUnit.DAYS))
        ).then();
    }

    private Mono<Void> createAuditLogIndexes() {
        log.info("AuditLog indexes are being created");
        ReactiveIndexOperations indexOps = mongoTemplate.indexOps(AuditLog.class);

        return Mono.when(
                indexOps.ensureIndex(new Index().on("userId", Sort.Direction.ASC)),
                indexOps.ensureIndex(new Index().on("serviceId", Sort.Direction.ASC)),
                indexOps.ensureIndex(new Index().on("logType", Sort.Direction.ASC)),
                indexOps.ensureIndex(new Index().on("resource", Sort.Direction.ASC)),
                indexOps.ensureIndex(new Index().on("status", Sort.Direction.ASC)),
                indexOps.ensureIndex(new Index().on("timestamp", Sort.Direction.DESC)),
                indexOps.ensureIndex(new Index().on("createdAt", Sort.Direction.DESC).expire(180, TimeUnit.DAYS))
        ).then();
    }

    private Mono<Void> createAdminActionIndexes() {
        log.info("AdminAction indexes are being created");
        ReactiveIndexOperations indexOps = mongoTemplate.indexOps(AdminAction.class);

        return Mono.when(
                indexOps.ensureIndex(new Index().on("adminId", Sort.Direction.ASC)),
                indexOps.ensureIndex(new Index().on("actionType", Sort.Direction.ASC)),
                indexOps.ensureIndex(new Index().on("targetId", Sort.Direction.ASC)),
                indexOps.ensureIndex(new Index().on("status", Sort.Direction.ASC)),
                indexOps.ensureIndex(new Index().on("timestamp", Sort.Direction.DESC))
        ).then();
    }
}
