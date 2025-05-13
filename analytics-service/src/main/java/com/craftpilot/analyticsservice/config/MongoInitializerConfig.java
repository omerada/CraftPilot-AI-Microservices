package com.craftpilot.analyticsservice.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexOperations;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class MongoInitializerConfig {

    private final ReactiveMongoTemplate mongoTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void initializeIndexes() {
        log.info("MongoDB indeksleri oluşturuluyor...");

        try {
            // Usage Metrics indeksleri
            createUsageMetricsIndexes();
            
            // Performance Metrics indeksleri
            createPerformanceMetricsIndexes();
            
            // Analytics Report indeksleri
            createAnalyticsReportIndexes();
            
            log.info("MongoDB indeksleri başarıyla oluşturuldu");
        } catch (Exception e) {
            log.warn("MongoDB indeksleri oluşturulurken hata: {}", e.getMessage());
        }
    }

    private void createUsageMetricsIndexes() {
        try {
            IndexOperations indexOps = mongoTemplate.indexOps("usage_metrics");
            
            indexOps.ensureIndex(new Index().on("userId", Sort.Direction.ASC))
                    .subscribe(result -> log.debug("Usage metrics userId indeksi oluşturuldu: {}", result),
                            error -> log.warn("Usage metrics userId indeksi oluşturulamadı: {}", error.getMessage()));
            
            indexOps.ensureIndex(new Index().on("timestamp", Sort.Direction.DESC))
                    .subscribe(result -> log.debug("Usage metrics timestamp indeksi oluşturuldu: {}", result),
                            error -> log.warn("Usage metrics timestamp indeksi oluşturulamadı: {}", error.getMessage()));
            
            indexOps.ensureIndex(new Index().on("serviceType", Sort.Direction.ASC))
                    .subscribe(result -> log.debug("Usage metrics serviceType indeksi oluşturuldu: {}", result),
                            error -> log.warn("Usage metrics serviceType indeksi oluşturulamadı: {}", error.getMessage()));
        } catch (Exception e) {
            log.warn("Usage metrics indeksleri oluşturulurken hata: {}", e.getMessage());
        }
    }

    private void createPerformanceMetricsIndexes() {
        try {
            IndexOperations indexOps = mongoTemplate.indexOps("performance_metrics");
            
            indexOps.ensureIndex(new Index().on("serviceId", Sort.Direction.ASC))
                    .subscribe(result -> log.debug("Performance metrics serviceId indeksi oluşturuldu: {}", result),
                            error -> log.warn("Performance metrics serviceId indeksi oluşturulamadı: {}", error.getMessage()));
            
            indexOps.ensureIndex(new Index().on("timestamp", Sort.Direction.DESC))
                    .subscribe(result -> log.debug("Performance metrics timestamp indeksi oluşturuldu: {}", result),
                            error -> log.warn("Performance metrics timestamp indeksi oluşturulamadı: {}", error.getMessage()));
            
            indexOps.ensureIndex(new Index().on("metricType", Sort.Direction.ASC))
                    .subscribe(result -> log.debug("Performance metrics metricType indeksi oluşturuldu: {}", result),
                            error -> log.warn("Performance metrics metricType indeksi oluşturulamadı: {}", error.getMessage()));
        } catch (Exception e) {
            log.warn("Performance metrics indeksleri oluşturulurken hata: {}", e.getMessage());
        }
    }

    private void createAnalyticsReportIndexes() {
        try {
            IndexOperations indexOps = mongoTemplate.indexOps("analytics_reports");
            
            indexOps.ensureIndex(new Index().on("reportName", Sort.Direction.ASC))
                    .subscribe(result -> log.debug("Analytics reports reportName indeksi oluşturuldu: {}", result),
                            error -> log.warn("Analytics reports reportName indeksi oluşturulamadı: {}", error.getMessage()));
            
            indexOps.ensureIndex(new Index().on("createdAt", Sort.Direction.DESC))
                    .subscribe(result -> log.debug("Analytics reports createdAt indeksi oluşturuldu: {}", result),
                            error -> log.warn("Analytics reports createdAt indeksi oluşturulamadı: {}", error.getMessage()));
            
            indexOps.ensureIndex(new Index().on("reportType", Sort.Direction.ASC))
                    .subscribe(result -> log.debug("Analytics reports reportType indeksi oluşturuldu: {}", result),
                            error -> log.warn("Analytics reports reportType indeksi oluşturulamadı: {}", error.getMessage()));
            
            indexOps.ensureIndex(new Index().on("tags", Sort.Direction.ASC))
                    .subscribe(result -> log.debug("Analytics reports tags indeksi oluşturuldu: {}", result),
                            error -> log.warn("Analytics reports tags indeksi oluşturulamadı: {}", error.getMessage()));
        } catch (Exception e) {
            log.warn("Analytics reports indeksleri oluşturulurken hata: {}", e.getMessage());
        }
    }
}
