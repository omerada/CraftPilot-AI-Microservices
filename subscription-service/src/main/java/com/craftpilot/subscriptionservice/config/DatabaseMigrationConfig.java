package com.craftpilot.subscriptionservice.config;

import com.craftpilot.subscriptionservice.model.payment.entity.Payment;
import com.craftpilot.subscriptionservice.model.subscription.entity.Plan;
import com.craftpilot.subscriptionservice.model.subscription.entity.Subscription;
import com.craftpilot.subscriptionservice.model.subscription.entity.SubscriptionPlan;
import com.craftpilot.subscriptionservice.repository.PaymentRepository;
import com.craftpilot.subscriptionservice.repository.PlanRepository;
import com.craftpilot.subscriptionservice.repository.SubscriptionPlanRepository;
import com.craftpilot.subscriptionservice.repository.SubscriptionRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.ReactiveIndexOperations;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Slf4j
@Configuration
@RequiredArgsConstructor
@Profile("!test") // Test profilinde çalıştırma
public class DatabaseMigrationConfig {

    private final ReactiveMongoTemplate mongoTemplate;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final PlanRepository planRepository;
    private final PaymentRepository paymentRepository;

    @Value("${spring.data.mongodb.auto-index-creation:true}")
    private boolean autoIndexCreation;

    @PostConstruct
    public void initializeDatabase() {
        log.info("Veritabanı indeksleri ve koleksiyonlar kontrol ediliyor");

        if (!autoIndexCreation) {
            log.info("Manuel indeks oluşturma işlemi başlatılıyor");
            createSubscriptionIndexes()
                    .then(createSubscriptionPlanIndexes())
                    .then(createPaymentIndexes())
                    .then(createPlanIndexes())
                    .doOnSuccess(v -> log.info("Tüm indeksler başarıyla oluşturuldu"))
                    .doOnError(e -> log.error("İndeks oluşturma hatası: {}", e.getMessage()))
                    .subscribe();
        } else {
            log.info("Otomatik indeks oluşturma etkin. Manuel indeks oluşturulmayacak.");
        }
    }

    private Mono<Void> createSubscriptionIndexes() {
        log.info("Subscription indeksleri oluşturuluyor");
        ReactiveIndexOperations indexOps = mongoTemplate.indexOps(Subscription.class);

        return Mono.when(
                indexOps.ensureIndex(new Index().on("userId", Sort.Direction.ASC).unique()),
                indexOps.ensureIndex(new Index().on("active", Sort.Direction.ASC)),
                indexOps.ensureIndex(new Index().on("endDate", Sort.Direction.ASC))).then();
    }

    private Mono<Void> createSubscriptionPlanIndexes() {
        log.info("SubscriptionPlan indeksleri oluşturuluyor");
        ReactiveIndexOperations indexOps = mongoTemplate.indexOps(SubscriptionPlan.class);

        return Mono.when(
                indexOps.ensureIndex(new Index().on("name", Sort.Direction.ASC)),
                indexOps.ensureIndex(new Index().on("isActive", Sort.Direction.ASC))).then();
    }

    private Mono<Void> createPaymentIndexes() {
        log.info("Payment indeksleri oluşturuluyor");
        ReactiveIndexOperations indexOps = mongoTemplate.indexOps(Payment.class);

        return Mono.when(
                indexOps.ensureIndex(new Index().on("userId", Sort.Direction.ASC)),
                indexOps.ensureIndex(new Index().on("subscriptionId", Sort.Direction.ASC)),
                indexOps.ensureIndex(new Index().on("status", Sort.Direction.ASC)),
                indexOps.ensureIndex(new Index().on("iyzicoPaymentTransactionId", Sort.Direction.ASC))).then();
    }

    private Mono<Void> createPlanIndexes() {
        log.info("Plan indeksleri oluşturuluyor");
        ReactiveIndexOperations indexOps = mongoTemplate.indexOps(Plan.class);

        return Mono.when(
                indexOps.ensureIndex(new Index().on("name", Sort.Direction.ASC)),
                indexOps.ensureIndex(new Index().on("isActive", Sort.Direction.ASC))).then();
    }
}
