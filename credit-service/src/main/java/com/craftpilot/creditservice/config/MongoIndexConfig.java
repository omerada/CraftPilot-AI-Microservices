package com.craftpilot.creditservice.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexInfo;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@Slf4j
@RequiredArgsConstructor
public class MongoIndexConfig {

    private final ReactiveMongoTemplate mongoTemplate;
    
    @Value("${mongodb.indexes.enabled:false}")
    private boolean indexesEnabled;

    @EventListener(ContextRefreshedEvent.class)
    public void initIndexes() {
        if (!indexesEnabled) {
            log.info("MongoDB indeks oluşturma işlemi devre dışı bırakılmıştır. İndeksler elle oluşturulmalıdır.");
            return;
        }
        
        log.info("Checking and initializing MongoDB indexes if needed");
        
        // Credit koleksiyonu için indeksler
        mongoTemplate.indexOps("credits").getIndexInfo()
            .collectList()
            .flatMap(indexInfos -> {
                if (indexExists(indexInfos, "userId")) {
                    log.info("Credit userId index already exists");
                    return Mono.empty();
                } else {
                    return mongoTemplate.indexOps("credits")
                            .ensureIndex(new Index().on("userId", Sort.Direction.ASC).unique().named("userId_index"))
                            .doOnSuccess(result -> log.info("Credit userId index created: {}", result));
                }
            }).subscribe();
        
        // CreditTransaction koleksiyonu için indeksler
        mongoTemplate.indexOps("credit_transactions").getIndexInfo()
            .collectList()
            .flatMap(indexInfos -> {
                if (indexExists(indexInfos, "userId")) {
                    log.info("CreditTransaction userId index already exists");
                    return Mono.empty();
                } else {
                    return mongoTemplate.indexOps("credit_transactions")
                            .ensureIndex(new Index().on("userId", Sort.Direction.ASC).named("userId_index"))
                            .doOnSuccess(result -> log.info("CreditTransaction userId index created: {}", result));
                }
            }).subscribe();
        
        mongoTemplate.indexOps("credit_transactions").getIndexInfo()
            .collectList()
            .flatMap(indexInfos -> {
                if (indexExists(indexInfos, "status")) {
                    log.info("CreditTransaction status index already exists");
                    return Mono.empty();
                } else {
                    return mongoTemplate.indexOps("credit_transactions")
                            .ensureIndex(new Index().on("status", Sort.Direction.ASC).named("status_index"))
                            .doOnSuccess(result -> log.info("CreditTransaction status index created: {}", result));
                }
            }).subscribe();
        
        mongoTemplate.indexOps("credit_transactions").getIndexInfo()
            .collectList()
            .flatMap(indexInfos -> {
                if (indexExists(indexInfos, "timestamp")) {
                    log.info("CreditTransaction timestamp index already exists");
                    return Mono.empty();
                } else {
                    return mongoTemplate.indexOps("credit_transactions")
                            .ensureIndex(new Index().on("timestamp", Sort.Direction.DESC).named("timestamp_index"))
                            .doOnSuccess(result -> log.info("CreditTransaction timestamp index created: {}", result));
                }
            }).subscribe();
        
        mongoTemplate.indexOps("credit_transactions").getIndexInfo()
            .collectList()
            .flatMap(indexInfos -> {
                if (indexExists(indexInfos, "type")) {
                    log.info("CreditTransaction type index already exists");
                    return Mono.empty();
                } else {
                    return mongoTemplate.indexOps("credit_transactions")
                            .ensureIndex(new Index().on("type", Sort.Direction.ASC).named("type_index"))
                            .doOnSuccess(result -> log.info("CreditTransaction type index created: {}", result));
                }
            }).subscribe();
        
        log.info("MongoDB indexes check completed");
    }
    
    private boolean indexExists(java.util.List<IndexInfo> indexInfos, String fieldName) {
        return indexInfos.stream()
                .anyMatch(indexInfo -> indexInfo.getIndexFields().stream()
                        .anyMatch(field -> field.getKey().equals(fieldName)));
    }
}
