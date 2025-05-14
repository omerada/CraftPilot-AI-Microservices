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

import javax.annotation.PostConstruct;
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

                // User koleksiyonu indeksleri
                createUserIndexes();

                // UserPreference koleksiyonu indeksleri
                createUserPreferenceIndexes();

                // AIModel koleksiyonu indeksleri
                createAIModelIndexes();

                // Provider koleksiyonu indeksleri
                createProviderIndexes();

                log.info("MongoDB indeksleri oluşturuldu");
        }

        @EventListener(ContextRefreshedEvent.class)
        public void initializeDatabase() {
                log.info("Veritabanı indeksleri ve koleksiyonlar kontrol ediliyor");

                if (!autoIndexCreation) {
                        createUserIndexes()
                                        .then(createUserPreferenceIndexes())
                                        .then(createProviderIndexes())
                                        .then(createAIModelIndexes())
                                        .doOnSuccess(v -> log.info("Tüm indeksler başarıyla oluşturuldu"))
                                        .doOnError(e -> log.error("İndeks oluşturma hatası: {}", e.getMessage()))
                                        .subscribe();
                }
        }

        private void createUserIndexes() {
                ReactiveIndexOperations indexOps = mongoTemplate.indexOps("users");

                // uid için unique indeks
                indexOps.ensureIndex(new Index().on("uid", Sort.Direction.ASC).unique())
                                .subscribe(
                                                result -> log.debug("User uid indeksi oluşturuldu: {}", result),
                                                error -> log.error("User uid indeksi oluşturulurken hata: {}",
                                                                error.getMessage()));

                // email için indeks
                indexOps.ensureIndex(new Index().on("email", Sort.Direction.ASC))
                                .subscribe(
                                                result -> log.debug("User email indeksi oluşturuldu: {}", result),
                                                error -> log.error("User email indeksi oluşturulurken hata: {}",
                                                                error.getMessage()));

                // createdAt için TTL indeksi (opsiyonel, uzun süre etkisiz kullanıcıları silmek
                // için)
                indexOps.ensureIndex(new Index().on("createdAt", Sort.Direction.ASC).expire(365, TimeUnit.DAYS))
                                .subscribe(
                                                result -> log.debug("User createdAt TTL indeksi oluşturuldu: {}",
                                                                result),
                                                error -> log.error("User createdAt TTL indeksi oluşturulurken hata: {}",
                                                                error.getMessage()));
        }

        private Mono<Void> createUserIndexes() {
                log.info("User indeksleri oluşturuluyor");
                ReactiveIndexOperations indexOps = mongoTemplate.indexOps(User.class);

                return Mono.when(
                                indexOps.ensureIndex(new Index().on("email", Sort.Direction.ASC).unique()),
                                indexOps.ensureIndex(new Index().on("uid", Sort.Direction.ASC).unique()),
                                indexOps.ensureIndex(new Index().on("createdAt", Sort.Direction.ASC))).then();
        }

        private void createUserPreferenceIndexes() {
                ReactiveIndexOperations indexOps = mongoTemplate.indexOps("userPreferences");

                // userId için unique indeks
                indexOps.ensureIndex(new Index().on("userId", Sort.Direction.ASC).unique())
                                .subscribe(
                                                result -> log.debug("UserPreference userId indeksi oluşturuldu: {}",
                                                                result),
                                                error -> log.error(
                                                                "UserPreference userId indeksi oluşturulurken hata: {}",
                                                                error.getMessage()));
        }

        private Mono<Void> createUserPreferenceIndexes() {
                log.info("UserPreference indeksleri oluşturuluyor");
                ReactiveIndexOperations indexOps = mongoTemplate.indexOps(UserPreference.class);

                return Mono.when(
                                indexOps.ensureIndex(new Index().on("userId", Sort.Direction.ASC).unique()),
                                indexOps.ensureIndex(new Index().on("lastUpdated", Sort.Direction.ASC))).then();
        }

        private void createAIModelIndexes() {
                ReactiveIndexOperations indexOps = mongoTemplate.indexOps("aiModels");

                // modelId için unique indeks
                indexOps.ensureIndex(new Index().on("modelId", Sort.Direction.ASC).unique())
                                .subscribe(
                                                result -> log.debug("AIModel modelId indeksi oluşturuldu: {}", result),
                                                error -> log.error("AIModel modelId indeksi oluşturulurken hata: {}",
                                                                error.getMessage()));

                // provider için indeks
                indexOps.ensureIndex(new Index().on("provider", Sort.Direction.ASC))
                                .subscribe(
                                                result -> log.debug("AIModel provider indeksi oluşturuldu: {}", result),
                                                error -> log.error("AIModel provider indeksi oluşturulurken hata: {}",
                                                                error.getMessage()));
        }

        private Mono<Void> createProviderIndexes() {
                log.info("Provider indeksleri oluşturuluyor");
                ReactiveIndexOperations indexOps = mongoTemplate.indexOps(Provider.class);

                return Mono.when(
                                indexOps.ensureIndex(new Index().on("name", Sort.Direction.ASC).unique()),
                                indexOps.ensureIndex(new Index().on("active", Sort.Direction.ASC))).then();
        }

        private void createProviderIndexes() {
                ReactiveIndexOperations indexOps = mongoTemplate.indexOps("providers");

                // providerId için unique indeks
                indexOps.ensureIndex(new Index().on("providerId", Sort.Direction.ASC).unique())
                                .subscribe(
                                                result -> log.debug("Provider providerId indeksi oluşturuldu: {}",
                                                                result),
                                                error -> log.error(
                                                                "Provider providerId indeksi oluşturulurken hata: {}",
                                                                error.getMessage()));
        }
}
