package com.craftpilot.activitylogservice.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
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
            log.info("MongoDB indeksleri oluşturma özelliği devre dışı bırakıldı. Mevcut indeksler kullanılacak.");
            return;
        }
        
        log.info("Activity log koleksiyonu için MongoDB indeksleri oluşturuluyor");
        
        // İndekslerin var olup olmadığını kontrol et
        mongoTemplate.indexOps("activity_logs").getIndexInfo()
            .collectList()
            .flatMap(indexInfo -> {
                if (!indexInfo.isEmpty()) {
                    log.info("MongoDB indeksleri zaten mevcut. Toplam {} indeks bulundu.", indexInfo.size());
                    return Mono.empty();
                }
                
                log.info("Hiç indeks bulunamadı. İndeksler oluşturuluyor...");
                return createAllIndexes();
            })
            .subscribe(
                result -> {},
                error -> log.error("İndeks kontrolü sırasında hata: {}", error.getMessage())
            );
    }
    
    private Mono<Void> createAllIndexes() {
        // userId indeksi
        mongoTemplate.indexOps("activity_logs")
                .ensureIndex(new Index().on("userId", Sort.Direction.ASC))
                .subscribe(
                    result -> log.info("userId indeksi oluşturuldu: {}", result),
                    error -> log.error("userId indeksi oluşturulurken hata: {}", error.getMessage())
                );
        
        // actionType indeksi
        mongoTemplate.indexOps("activity_logs")
                .ensureIndex(new Index().on("actionType", Sort.Direction.ASC))
                .subscribe(
                    result -> log.info("actionType indeksi oluşturuldu: {}", result),
                    error -> log.error("actionType indeksi oluşturulurken hata: {}", error.getMessage())
                );
        
        // timestamp indeksi - DESC sıralı sorgular için optimize
        mongoTemplate.indexOps("activity_logs")
                .ensureIndex(new Index().on("timestamp", Sort.Direction.DESC))
                .subscribe(
                    result -> log.info("timestamp indeksi oluşturuldu: {}", result),
                    error -> log.error("timestamp indeksi oluşturulurken hata: {}", error.getMessage())
                );
        
        // Bileşik indeks - userId ve timestamp için
        mongoTemplate.indexOps("activity_logs")
                .ensureIndex(new Index()
                    .on("userId", Sort.Direction.ASC)
                    .on("timestamp", Sort.Direction.DESC))
                .subscribe(
                    result -> log.info("userId+timestamp bileşik indeksi oluşturuldu: {}", result),
                    error -> log.error("userId+timestamp bileşik indeksi oluşturulurken hata: {}", error.getMessage())
                );
        
        // 30 günden eski activity log kayıtlarını otomatik silmek için TTL indeksi
        mongoTemplate.indexOps("activity_logs")
                .ensureIndex(new Index()
                    .on("timestamp", Sort.Direction.ASC)
                    .expire(2592000)) // 30 gün = 2592000 saniye
                .subscribe(
                    result -> log.info("timestamp TTL indeksi oluşturuldu: {}", result),
                    error -> log.error("timestamp TTL indeksi oluşturulurken hata: {}", error.getMessage())
                );
                
        log.info("Activity log MongoDB indeksleri oluşturma talebi gönderildi");
        
        return Mono.empty();
    }
}
