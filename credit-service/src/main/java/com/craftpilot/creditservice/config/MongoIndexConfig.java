package com.craftpilot.creditservice.config;

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
        log.info("Initializing MongoDB indexes");
        
        // Credit koleksiyonu için indeksler
        mongoTemplate.indexOps("credits")
                .ensureIndex(new Index().on("userId", Sort.Direction.ASC).unique())
                .subscribe(result -> log.info("Credit userId index created: {}", result));
        
        // CreditTransaction koleksiyonu için indeksler
        mongoTemplate.indexOps("credit_transactions")
                .ensureIndex(new Index().on("userId", Sort.Direction.ASC))
                .subscribe(result -> log.info("CreditTransaction userId index created: {}", result));
        
        mongoTemplate.indexOps("credit_transactions")
                .ensureIndex(new Index().on("status", Sort.Direction.ASC))
                .subscribe(result -> log.info("CreditTransaction status index created: {}", result));
        
        mongoTemplate.indexOps("credit_transactions")
                .ensureIndex(new Index().on("timestamp", Sort.Direction.DESC))
                .subscribe(result -> log.info("CreditTransaction timestamp index created: {}", result));
        
        mongoTemplate.indexOps("credit_transactions")
                .ensureIndex(new Index().on("type", Sort.Direction.ASC))
                .subscribe(result -> log.info("CreditTransaction type index created: {}", result));
        
        log.info("MongoDB indexes initialization completed");
    }
}
