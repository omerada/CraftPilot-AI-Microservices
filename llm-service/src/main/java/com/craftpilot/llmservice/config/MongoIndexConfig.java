package com.craftpilot.llmservice.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@Slf4j
@RequiredArgsConstructor
public class MongoIndexConfig {

    private final ReactiveMongoTemplate mongoTemplate;

    @EventListener(ContextRefreshedEvent.class)
    public void initIndexes() {
        log.info("MongoDB indeksleri başlatılıyor");

        try {
            // ChatHistory koleksiyonu için indeksler
            createReactiveIndex("chat_histories", "userId", Sort.Direction.ASC, false);
            createReactiveIndex("chat_histories", "sessionId", Sort.Direction.ASC, false);
            createReactiveIndex("chat_histories", "createdAt", Sort.Direction.DESC, false);
            createReactiveIndex("chat_histories", "updatedAt", Sort.Direction.DESC, false);
            createReactiveIndex("chat_histories", "aiModel", Sort.Direction.ASC, false);

            // PerformanceAnalysis koleksiyonu için indeksler
            createReactiveIndex("performance_analyses", "modelId", Sort.Direction.ASC, false);
            createReactiveIndex("performance_analyses", "sessionId", Sort.Direction.ASC, false);
            createReactiveIndex("performance_analyses", "userId", Sort.Direction.ASC, false);
            createReactiveIndex("performance_analyses", "timestamp", Sort.Direction.DESC, false);

            // PerformanceAnalysisResponse koleksiyonu için indeksler
            createReactiveIndex("performance_analysis_responses", "modelId", Sort.Direction.ASC, false);
            createReactiveIndex("performance_analysis_responses", "sessionId", Sort.Direction.ASC, false);
            createReactiveIndex("performance_analysis_responses", "timestamp", Sort.Direction.DESC, false);
            createReactiveIndex("performance_analysis_responses", "url", Sort.Direction.ASC, false);
            createReactiveIndex("performance_analysis_responses", "status", Sort.Direction.ASC, false);

            log.info("MongoDB indeks başlatma işlemi tamamlandı");
        } catch (Exception e) {
            // Uygulama başlangıcının indeks sorunlarından dolayı başarısız olmasını engelle
            log.error("MongoDB indeksleri oluşturulurken bir hata oluştu, ancak uygulama çalışmaya devam edecek: {}",
                    e.getMessage());
        }
    }

    private void createReactiveIndex(String collection, String field, Sort.Direction direction, boolean unique) {
        Index index = new Index().on(field, direction);

        if (unique) {
            index = index.unique();
        }

        mongoTemplate.indexOps(collection)
                .ensureIndex(index)
                .doOnSuccess(result -> log.info("{} koleksiyonunda {} indeksi oluşturuldu: {}",
                        collection, field, result))
                .doOnError(e -> log.warn("{} alanı için indeks oluşturulamadı: {}", field, e.getMessage()))
                .subscribe();
    }
}
