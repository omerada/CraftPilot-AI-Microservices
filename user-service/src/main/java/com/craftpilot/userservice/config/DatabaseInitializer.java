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

        @PostConstruct
        public void initIndexes() {
                log.info("MongoDB indekslerini oluşturma işlemi başlıyor...");

                // İndeksleme kontrolü
                if (!autoIndexCreation) {
                    createUserIndexes()
                            .then(createUserPreferenceIndexes())
                            .then(createProviderIndexes())
                            .then(createAIModelIndexes())
                            .doOnSuccess(v -> log.info("Tüm indeksler başarıyla oluşturuldu"))
                            .doOnError(e -> log.error("İndeks oluşturma hatası: {}", e.getMessage()))
                            .subscribe();
                } else {
                    log.info("spring.data.mongodb.auto-index-creation etkin. İndeksler otomatik oluşturulacak.");
                }
                
                log.info("MongoDB indeksleri oluşturuldu");
        }

        @EventListener(ContextRefreshedEvent.class)
        public void initializeDatabase() {
                log.info("Veritabanı indeksleri ve koleksiyonlar kontrol ediliyor");
        }

        private Mono<Void> createUserIndexes() {
                log.info("User indeksleri oluşturuluyor");
                ReactiveIndexOperations indexOps = mongoTemplate.indexOps(User.class);

                return Mono.when(
                                indexOps.ensureIndex(new Index().on("email", Sort.Direction.ASC).unique()),
                                indexOps.ensureIndex(new Index().on("uid", Sort.Direction.ASC).unique()),
                                indexOps.ensureIndex(new Index().on("createdAt", Sort.Direction.ASC))).then();
        }

        private Mono<Void> createUserPreferenceIndexes() {
                log.info("UserPreference indeksleri oluşturuluyor");
                ReactiveIndexOperations indexOps = mongoTemplate.indexOps(UserPreference.class);

                return Mono.when(
                                indexOps.ensureIndex(new Index().on("userId", Sort.Direction.ASC).unique()),
                                indexOps.ensureIndex(new Index().on("lastUpdated", Sort.Direction.ASC))).then();
        }

        private Mono<Void> createAIModelIndexes() {
                log.info("AIModel indeksleri oluşturuluyor");
                ReactiveIndexOperations indexOps = mongoTemplate.indexOps(AIModel.class);

                return Mono.when(
                                indexOps.ensureIndex(new Index().on("modelId", Sort.Direction.ASC).unique()),
                                indexOps.ensureIndex(new Index().on("provider", Sort.Direction.ASC)),
                                indexOps.ensureIndex(new Index().on("category", Sort.Direction.ASC))).then();
        }

        private Mono<Void> createProviderIndexes() {
                log.info("Provider indeksleri oluşturuluyor");
                ReactiveIndexOperations indexOps = mongoTemplate.indexOps(Provider.class);

                return Mono.when(
                                indexOps.ensureIndex(new Index().on("name", Sort.Direction.ASC).unique()),
                                indexOps.ensureIndex(new Index().on("active", Sort.Direction.ASC))).then();
        }
}
