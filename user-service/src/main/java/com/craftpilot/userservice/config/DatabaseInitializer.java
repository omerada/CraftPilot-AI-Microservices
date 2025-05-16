package com.craftpilot.userservice.config;

import com.craftpilot.userservice.model.User;
import com.craftpilot.userservice.model.UserPreference;
import com.craftpilot.userservice.model.ai.AIModel;
import com.craftpilot.userservice.model.ai.Provider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.ReactiveIndexOperations;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
@Configuration
public class DatabaseInitializer {

        private final ReactiveMongoTemplate mongoTemplate;

        @Value("${spring.data.mongodb.auto-index-creation:false}")
        private boolean autoIndexCreation;
        
        @Value("${app.mongodb.create-indexes:false}")
        private boolean createIndexes;

        @PostConstruct
        public void initIndexes() {
                if (log.isDebugEnabled()) {
                    log.debug("Checking MongoDB index configuration");
                }

                // Index creation is now controlled by both Spring's auto-index-creation 
                // and our custom property - disable by default
                if (createIndexes && !autoIndexCreation) {
                    if (log.isDebugEnabled()) {
                        log.debug("Creating MongoDB indexes manually");
                    }
                    
                    createUserIndexes()
                            .then(createUserPreferenceIndexes())
                            .then(createProviderIndexes())
                            .then(createAIModelIndexes())
                            .doOnSuccess(v -> log.debug("All indexes created successfully"))
                            .doOnError(e -> log.error("Error creating indexes: {}", e.getMessage()))
                            .subscribe();
                } else if (log.isDebugEnabled()) {
                    log.debug("Index creation skipped: createIndexes={}, autoIndexCreation={}", 
                             createIndexes, autoIndexCreation);
                }
        }

        @EventListener(ContextRefreshedEvent.class)
        public void initializeDatabase() {
                if (log.isDebugEnabled()) {
                    log.debug("Database initialization complete");
                }
        }

        private Mono<Void> createUserIndexes() {
                if (log.isDebugEnabled()) {
                    log.debug("Creating User indexes");
                }
                ReactiveIndexOperations indexOps = mongoTemplate.indexOps(User.class);

                return Mono.when(
                                indexOps.ensureIndex(new Index().on("email", Sort.Direction.ASC).unique()),
                                indexOps.ensureIndex(new Index().on("uid", Sort.Direction.ASC).unique()),
                                indexOps.ensureIndex(new Index().on("createdAt", Sort.Direction.ASC))).then();
        }

        private Mono<Void> createUserPreferenceIndexes() {
                if (log.isDebugEnabled()) {
                    log.debug("Creating UserPreference indexes");
                }
                ReactiveIndexOperations indexOps = mongoTemplate.indexOps(UserPreference.class);

                return Mono.when(
                                indexOps.ensureIndex(new Index().on("userId", Sort.Direction.ASC).unique()),
                                indexOps.ensureIndex(new Index().on("lastUpdated", Sort.Direction.ASC))).then();
        }

        private Mono<Void> createAIModelIndexes() {
                if (log.isDebugEnabled()) {
                    log.debug("Creating AIModel indexes");
                }
                ReactiveIndexOperations indexOps = mongoTemplate.indexOps(AIModel.class);

                return Mono.when(
                                indexOps.ensureIndex(new Index().on("modelId", Sort.Direction.ASC).unique()),
                                indexOps.ensureIndex(new Index().on("provider", Sort.Direction.ASC)),
                                indexOps.ensureIndex(new Index().on("category", Sort.Direction.ASC))).then();
        }

        private Mono<Void> createProviderIndexes() {
                if (log.isDebugEnabled()) {
                    log.debug("Creating Provider indexes");
                }
                ReactiveIndexOperations indexOps = mongoTemplate.indexOps(Provider.class);

                return Mono.when(
                                indexOps.ensureIndex(new Index().on("name", Sort.Direction.ASC).unique()),
                                indexOps.ensureIndex(new Index().on("active", Sort.Direction.ASC))).then();
        }
}
