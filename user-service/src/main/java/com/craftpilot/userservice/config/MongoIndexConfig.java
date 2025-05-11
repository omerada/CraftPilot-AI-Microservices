package com.craftpilot.userservice.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import reactor.core.publisher.Mono;

import jakarta.annotation.PostConstruct;
import java.util.concurrent.TimeUnit;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class MongoIndexConfig {

    private final ReactiveMongoTemplate mongoTemplate;

    @PostConstruct
    public void initIndexes() {
        log.info("Initializing MongoDB indexes");
        
        // User collection indexes
        createUserIndexes()
            .doOnSuccess(v -> log.info("User indexes created successfully"))
            .doOnError(e -> log.error("Failed to create user indexes", e))
            .subscribe();
        
        // UserPreference collection indexes
        createUserPreferenceIndexes()
            .doOnSuccess(v -> log.info("UserPreference indexes created successfully"))
            .doOnError(e -> log.error("Failed to create UserPreference indexes", e))
            .subscribe();
    }

    private Mono<Void> createUserIndexes() {
        return Mono.defer(() -> {
            // Username'e unique index ekle
            Index usernameIndex = new Index()
                    .on("username", Sort.Direction.ASC)
                    .unique()
                    .named("idx_username_unique");
            
            // Email'e unique index ekle
            Index emailIndex = new Index()
                    .on("email", Sort.Direction.ASC)
                    .unique()
                    .named("idx_email_unique");
            
            // Status ve createdAt için bileşik index
            Index statusDateIndex = new Index()
                    .on("status", Sort.Direction.ASC)
                    .on("createdAt", Sort.Direction.DESC)
                    .named("idx_status_createdAt");
            
            // TTL index for session management (if needed)
            Index sessionTtlIndex = new Index()
                    .on("lastAccessedAt", Sort.Direction.ASC)
                    .expire(30, TimeUnit.DAYS)
                    .named("idx_session_ttl");
            
            return mongoTemplate.indexOps("users").ensureIndex(usernameIndex)
                    .then(mongoTemplate.indexOps("users").ensureIndex(emailIndex))
                    .then(mongoTemplate.indexOps("users").ensureIndex(statusDateIndex))
                    .then(mongoTemplate.indexOps("users").ensureIndex(sessionTtlIndex))
                    .then();
        });
    }
    
    private Mono<Void> createUserPreferenceIndexes() {
        return Mono.defer(() -> {
            // userId'ye unique index ekle
            Index userIdIndex = new Index()
                    .on("userId", Sort.Direction.ASC)
                    .unique()
                    .named("idx_userId_unique");
            
            // language ve lastUpdated için index
            Index langUpdateIndex = new Index()
                    .on("language", Sort.Direction.ASC)
                    .on("lastUpdated", Sort.Direction.DESC)
                    .named("idx_lang_updated");
                    
            return mongoTemplate.indexOps("userPreferences").ensureIndex(userIdIndex)
                    .then(mongoTemplate.indexOps("userPreferences").ensureIndex(langUpdateIndex))
                    .then();
        });
    }
}
