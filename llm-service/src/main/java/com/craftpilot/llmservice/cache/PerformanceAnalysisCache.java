package com.craftpilot.llmservice.cache;

import com.craftpilot.llmservice.model.performance.PerformanceAnalysisResponse;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.function.Function;

@Component
@Slf4j
public class PerformanceAnalysisCache {
    private final Cache<String, PerformanceAnalysisResponse> cache;
    
    public PerformanceAnalysisCache() {
        this.cache = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(5)) // 5 dakika önbellek süresi
                .maximumSize(100) // Maksimum 100 URL'in önbellekte tutulması
                .recordStats() // İstatistikleri kaydet
                .build();
    }
    
    public Mono<PerformanceAnalysisResponse> getAnalysisResult(String url) {
        PerformanceAnalysisResponse cachedResult = cache.getIfPresent(url);
        if (cachedResult != null) {
            log.debug("Cache hit for performance analysis: {}", url);
            return Mono.just(cachedResult);
        } else {
            log.debug("Cache miss for performance analysis: {}", url);
            return Mono.empty();
        }
    }
    
    public void cacheAnalysisResult(String url, PerformanceAnalysisResponse result) {
        cache.put(url, result);
        log.debug("URL cached: {}", url);
    }
    
    public void invalidate(String url) {
        cache.invalidate(url);
        log.debug("URL cache invalidated: {}", url);
    }
}
