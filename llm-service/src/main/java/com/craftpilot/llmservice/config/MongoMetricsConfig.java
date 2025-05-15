package com.craftpilot.llmservice.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.beans.factory.annotation.Value;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class MongoMetricsConfig {

    private final ReactiveMongoTemplate mongoTemplate;
    private final MeterRegistry meterRegistry;
    
    @Value("${mongodb.metrics.enabled:true}")
    private boolean metricsEnabled;

    private final AtomicReference<Double> connectionPoolSize = new AtomicReference<>(0.0);

    @Bean
    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
        return registry -> registry.config().commonTags("application", "llm-service");
    }

    @Bean
    public MongoMetricsRegistry mongoMetrics(ReactiveMongoTemplate mongoTemplate) {
        // getDatabaseName() metodu yerine MongoClient üzerinden database adını al
        String databaseName = mongoTemplate.getMongoDatabase().block().getName();

        return new MongoMetricsRegistry(databaseName,
                Collections.emptyList(),
                meterRegistry);
    }

    @Scheduled(fixedRate = 60000) // Her 1 dakikada bir
    public void collectMongoMetrics() {
        if (!metricsEnabled) {
            return;
        }
        
        mongoTemplate.executeCommand("{ serverStatus: 1 }")
                .doOnNext(serverStatus -> {
                    try {
                        // Bağlantı havuzu metrikleri
                        if (serverStatus.containsKey("connections")) {
                            Object connectionsObj = serverStatus.get("connections");
                            if (connectionsObj instanceof org.bson.Document) {
                                org.bson.Document connections = (org.bson.Document) connectionsObj;
                                double current = connections.getInteger("current", 0);
                                double available = connections.getInteger("available", 0);

                                meterRegistry.gauge("mongodb.connections.current",
                                        Tags.of("database", mongoTemplate.getMongoDatabase().block().getName()),
                                        connectionPoolSize,
                                        ref -> current);

                                if (log.isTraceEnabled()) {
                                    log.trace("MongoDB bağlantı metrikleri güncellendi: mevcut={}, kullanılabilir={}",
                                            current, available);
                                }
                            }
                        }
                    } catch (Exception e) {
                        log.warn("MongoDB metrikleri alınırken hata oluştu: {}", e.getMessage());
                    }
                })
                .doOnError(e -> log.error("MongoDB sunucu durumu alınamadı: {}", e.getMessage()))
                .subscribe();
    }
}
