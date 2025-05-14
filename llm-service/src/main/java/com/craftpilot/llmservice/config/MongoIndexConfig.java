package com.craftpilot.llmservice.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
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
            createIndex("chat_histories", "userId", Sort.Direction.ASC, false);
            createIndex("chat_histories", "sessionId", Sort.Direction.ASC, false);
            createIndex("chat_histories", "timestamp", Sort.Direction.DESC, false);

            // PerformanceAnalysis koleksiyonu için indeksler
            createIndex("performance_analyses", "modelId", Sort.Direction.ASC, false);
            createIndex("performance_analyses", "sessionId", Sort.Direction.ASC, false);
            createIndex("performance_analyses", "timestamp", Sort.Direction.DESC, false);

            log.info("MongoDB indeks başlatma işlemi tamamlandı");
        } catch (Exception e) {
            // Uygulama başlangıcının indeks sorunlarından dolayı başarısız olmasını engelle
            log.error("MongoDB indeksleri oluşturulurken bir hata oluştu, ancak uygulama çalışmaya devam edecek: {}",
                    e.getMessage());
        }
    }

    private void createIndex(String collection, String field, Sort.Direction direction, boolean unique) {
        Index index = new Index().on(field, direction);

        // Eğer index benzersiz olacaksa unique() metodunu çağır
        if (unique) {
            index = index.unique();
        }

        mongoTemplate.indexOps(collection)
                .ensureIndex(index)
                .onErrorResume(e -> {
                    log.warn("{} koleksiyonunda {} alanı için indeks oluşturulamadı: {}",
                            collection, field, e.getMessage());
                    return Mono.empty();
                })
                .subscribe(result -> log.info("{} koleksiyonunda {} indeksi oluşturuldu: {}",
                        collection, field, result));
    }
}
