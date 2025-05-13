package com.craftpilot.imageservice.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class MongoIndexConfig {

    private final ReactiveMongoTemplate mongoTemplate;

    @EventListener(ContextRefreshedEvent.class)
    public void initIndexes() {
        log.info("MongoDB indeksleri başlatılıyor");
        
        // Image koleksiyonu için indeksler
        mongoTemplate.indexOps("images")
                .ensureIndex(new Index().on("userId", Sort.Direction.ASC))
                .subscribe(result -> log.info("Image userId indeksi oluşturuldu: {}", result));
        
        mongoTemplate.indexOps("images")
                .ensureIndex(new Index().on("createdAt", Sort.Direction.DESC))
                .subscribe(result -> log.info("Image createdAt indeksi oluşturuldu: {}", result));
        
        mongoTemplate.indexOps("images")
                .ensureIndex(new Index().on("status", Sort.Direction.ASC))
                .subscribe(result -> log.info("Image status indeksi oluşturuldu: {}", result));
        
        mongoTemplate.indexOps("images")
                .ensureIndex(new Index().on("tags", Sort.Direction.ASC))
                .subscribe(result -> log.info("Image tags indeksi oluşturuldu: {}", result));
        
        log.info("MongoDB indeks başlatma işlemi tamamlandı");
    }
}
