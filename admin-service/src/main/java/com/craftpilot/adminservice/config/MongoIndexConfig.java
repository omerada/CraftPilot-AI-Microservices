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

    @PostConstruct
    public void initializeDatabase() {
        log.info("MongoDB index configuration initialized with auto-index-creation: {}", autoIndexCreation);
    }

    @EventListener(ContextRefreshedEvent.class)
    public void initIndices() {
        log.info("MongoDB indekslerini oluşturma işlemi başlıyor...");

        if (!autoIndexCreation) {
            log.info("Auto index creation is disabled. Creating indexes manually.");
            createUserActivityIndexes()
                    .then(createSystemMetricsIndexes())
                    .then(createSystemAlertIndexes())
                    .then(createAuditLogIndexes())
                    .then(createAdminActionIndexes())
                    .doOnSuccess(v -> log.info("Tüm MongoDB indeksleri başarıyla oluşturuldu"))
                    .doOnError(e -> log.error("MongoDB indeksleri oluşturulurken hata: {}", e.getMessage()))
                    .subscribe();
        } else {
            log.info("Auto index creation is enabled. Spring Data MongoDB will handle index creation.");
        }
    }

    private Mono<Void> createUserActivityIndexes() {
        log.info("UserActivity indeksleri oluşturuluyor");
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
        log.info("SystemMetrics indeksleri oluşturuluyor");
        ReactiveIndexOperations indexOps = mongoTemplate.indexOps(SystemMetrics.class);

        return Mono.when(
                indexOps.ensureIndex(new Index().on("serviceId", Sort.Direction.ASC).unique()),
                indexOps.ensureIndex(new Index().on("serviceType", Sort.Direction.ASC)),
                indexOps.ensureIndex(new Index().on("status", Sort.Direction.ASC)),
                indexOps.ensureIndex(new Index().on("timestamp", Sort.Direction.DESC))
        ).then();
    }

    private Mono<Void> createSystemAlertIndexes() {
        log.info("SystemAlert indeksleri oluşturuluyor");
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
        log.info("AuditLog indeksleri oluşturuluyor");
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
        log.info("AdminAction indeksleri oluşturuluyor");
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
