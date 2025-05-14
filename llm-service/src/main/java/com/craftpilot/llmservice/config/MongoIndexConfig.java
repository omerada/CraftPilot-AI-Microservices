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
            IndexOperations chatHistoryIndexOps = mongoTemplate.indexOps("chat_histories");
            createAndEnsureIndex(chatHistoryIndexOps, "userId", Sort.Direction.ASC, false);
            createAndEnsureIndex(chatHistoryIndexOps, "sessionId", Sort.Direction.ASC, false);
            createAndEnsureIndex(chatHistoryIndexOps, "createdAt", Sort.Direction.DESC, false);
            createAndEnsureIndex(chatHistoryIndexOps, "updatedAt", Sort.Direction.DESC, false);
            createAndEnsureIndex(chatHistoryIndexOps, "aiModel", Sort.Direction.ASC, false);

            // PerformanceAnalysis koleksiyonu için indeksler
            IndexOperations perfAnalysisIndexOps = mongoTemplate.indexOps("performance_analyses");
            createAndEnsureIndex(perfAnalysisIndexOps, "modelId", Sort.Direction.ASC, false);
            createAndEnsureIndex(perfAnalysisIndexOps, "sessionId", Sort.Direction.ASC, false);
            createAndEnsureIndex(perfAnalysisIndexOps, "userId", Sort.Direction.ASC, false);
            createAndEnsureIndex(perfAnalysisIndexOps, "timestamp", Sort.Direction.DESC, false);

            // PerformanceAnalysisResponse koleksiyonu için indeksler
            IndexOperations perfAnalysisRespIndexOps = mongoTemplate.indexOps("performance_analysis_responses");
            createAndEnsureIndex(perfAnalysisRespIndexOps, "modelId", Sort.Direction.ASC, false);
            createAndEnsureIndex(perfAnalysisRespIndexOps, "sessionId", Sort.Direction.ASC, false);
            createAndEnsureIndex(perfAnalysisRespIndexOps, "timestamp", Sort.Direction.DESC, false);
            createAndEnsureIndex(perfAnalysisRespIndexOps, "url", Sort.Direction.ASC, false);
            createAndEnsureIndex(perfAnalysisRespIndexOps, "status", Sort.Direction.ASC, false);

            log.info("MongoDB indeks başlatma işlemi tamamlandı");
        } catch (Exception e) {
            // Uygulama başlangıcının indeks sorunlarından dolayı başarısız olmasını engelle
            log.error("MongoDB indeksleri oluşturulurken bir hata oluştu, ancak uygulama çalışmaya devam edecek: {}",
                    e.getMessage());
        }
    }

    private void createAndEnsureIndex(IndexOperations indexOps, String field, Sort.Direction direction,
            boolean unique) {
        Index index = new Index().on(field, direction);

        if (unique) {
            index = index.unique();
        }

        indexOps.ensureIndex(index)
                .onErrorResume(e -> {
                    log.warn("{} alanı için indeks oluşturulamadı: {}", field, e.getMessage());
                    return Mono.empty();
                })
                .subscribe(result -> log.info("{} indeksi oluşturuldu: {}", field, result));
    }
}
