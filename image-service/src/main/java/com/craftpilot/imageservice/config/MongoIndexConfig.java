package com.craftpilot.imageservice.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
    
    @Value("${mongodb.create-indexes:false}")
    private boolean createIndexes;

    @EventListener(ContextRefreshedEvent.class)
    public void initIndexes() {
        if (!createIndexes) {
            log.info("MongoDB indeks oluşturma devre dışı bırakıldı, indeksler zaten mevcut");
            return;
        }
        
        log.info("MongoDB indeksleri başlatılıyor");
        
        try {
            // Image koleksiyonu için indeksler
            createIndex("images", "userId", Sort.Direction.ASC, false);
            createIndex("images", "createdAt", Sort.Direction.DESC, false);
            createIndex("images", "status", Sort.Direction.ASC, false);
            createIndex("images", "tags", Sort.Direction.ASC, false);
            
            log.info("MongoDB indeks başlatma işlemi tamamlandı");
        } catch (Exception e) {
            // Uygulama başlangıcının indeks sorunlarından dolayı başarısız olmasını engelle
            log.error("MongoDB indeksleri oluşturulurken bir hata oluştu, ancak uygulama çalışmaya devam edecek: {}", e.getMessage());
        }
    }
    
    private void createIndex(String collection, String field, Sort.Direction direction, boolean unique) {
        Index index = new Index().on(field, direction);
        
        // Eğer index benzersiz olacaksa unique() metodunu parametresiz çağır
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
                .subscribe(result -> 
                        log.info("{} koleksiyonunda {} indeksi oluşturuldu: {}", 
                                collection, field, result));
    }
}
