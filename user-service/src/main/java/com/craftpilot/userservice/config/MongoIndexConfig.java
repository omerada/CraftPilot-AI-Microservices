package com.craftpilot.userservice.config;

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
        log.info("Initializing MongoDB indexes for user service");
        
        // Users koleksiyonu için indeksler
        mongoTemplate.indexOps("users")
                .ensureIndex(new Index().on("email", Sort.Direction.ASC).unique())
                .subscribe(result -> log.info("User email index created: {}", result));
        
        mongoTemplate.indexOps("users")
                .ensureIndex(new Index().on("username", Sort.Direction.ASC).unique())
                .subscribe(result -> log.info("User username index created: {}", result));
        
        mongoTemplate.indexOps("users")
                .ensureIndex(new Index().on("role", Sort.Direction.ASC))
                .subscribe(result -> log.info("User role index created: {}", result));
        
        mongoTemplate.indexOps("users")
                .ensureIndex(new Index().on("status", Sort.Direction.ASC))
                .subscribe(result -> log.info("User status index created: {}", result));
        
        mongoTemplate.indexOps("users")
                .ensureIndex(new Index().on("createdAt", Sort.Direction.DESC))
                .subscribe(result -> log.info("User createdAt index created: {}", result));
        
        // User preferences koleksiyonu için indeksler
        mongoTemplate.indexOps("user_preferences")
                .ensureIndex(new Index().on("userId", Sort.Direction.ASC).unique())
                .subscribe(result -> log.info("UserPreference userId index created: {}", result));
        
        log.info("MongoDB indexes initialization completed for user service");
    }
}
