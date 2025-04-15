package com.craftpilot.lighthouseservice.service;

import com.craftpilot.lighthouseservice.model.JobStatusResponse;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Slf4j
public class LighthouseWorkerService {
    private final ReactiveRedisTemplate<String, Object> redisTemplate;
    
    @Value("${lighthouse.queue.name}")
    private String queueName;
    
    @Value("${lighthouse.results.prefix}")
    private String resultsPrefix;
    
    @Value("${lighthouse.worker.count:3}")
    private int workerCount;
    
    @Value("${lighthouse.worker.poll-interval:1000}")
    private int pollInterval; // ms
    
    private ExecutorService workerPool;
    private final AtomicInteger activeWorkers = new AtomicInteger(0);
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final Map<String, Long> processingJobs = new ConcurrentHashMap<>();
    
    @PostConstruct
    public void init() {
        log.info("Initializing Lighthouse worker service with {} workers", workerCount);
        workerPool = Executors.newFixedThreadPool(workerCount);
        
        // İlk worker'ları başlat
        for (int i = 0; i < workerCount; i++) {
            startWorker(i);
        }
    }
    
    @PreDestroy
    public void shutdown() {
        log.info("Shutting down Lighthouse worker service");
        running.set(false);
        workerPool.shutdown();
    }
    
    @Scheduled(fixedDelayString = "${lighthouse.worker.check-interval:10000}")
    public void checkAndProcessQueue() {
        // Kuyrukta bekleyen işleri işlemeye başla
        if (activeWorkers.get() < workerCount) {
            log.debug("Active workers: {}/{}, starting more workers", activeWorkers.get(), workerCount);
            for (int i = activeWorkers.get(); i < workerCount; i++) {
                startWorker(i);
            }
        }
    }
    
    private void startWorker(int workerId) {
        final String workerName = "worker-" + workerId;
        
        log.info("Starting Lighthouse worker: {}", workerName);
        
        workerPool.submit(() -> {
            try {
                activeWorkers.incrementAndGet();
                log.info("{} started, active workers: {}/{}", workerName, activeWorkers.get(), workerCount);
                
                while (running.get()) {
                    try {
                        // Redis'ten bir job al
                        redisTemplate.opsForList().leftPop(queueName)
                            .subscribeOn(Schedulers.boundedElastic())
                            .doOnNext(job -> {
                                try {
                                    if (job instanceof Map) {
                                        @SuppressWarnings("unchecked")
                                        Map<String, Object> jobMap = (Map<String, Object>) job;
                                        String jobId = (String) jobMap.get("id");
                                        String url = (String) jobMap.get("url");
                                        
                                        if (jobId != null && url != null) {
                                            processJob(jobId, url, jobMap, workerName);
                                        } else {
                                            log.warn("{} received invalid job: {}", workerName, jobMap);
                                        }
                                    } else {
                                        log.warn("{} received job of unexpected type: {}", workerName, 
                                            job != null ? job.getClass().getName() : "null");
                                    }
                                } catch (Exception e) {
                                    log.error("{} error processing job: {}", workerName, e.getMessage(), e);
                                }
                            })
                            .block(Duration.ofSeconds(30));
                        
                        // Yeni bir iş yoksa biraz bekle
                        Thread.sleep(pollInterval);
                    } catch (Exception e) {
                        log.error("{} error in worker loop: {}", workerName, e.getMessage(), e);
                        
                        // Ciddi bir hata durumunda daha uzun süre bekle
                        Thread.sleep(5000);
                    }
                }
            } catch (InterruptedException e) {
                log.info("{} was interrupted, shutting down", workerName);
                Thread.currentThread().interrupt();
            } finally {
                activeWorkers.decrementAndGet();
                log.info("{} stopped, remaining active workers: {}", workerName, activeWorkers.get());
            }
        });
    }
    
    private void processJob(String jobId, String url, Map<String, Object> jobMap, String workerName) {
        try {
            // İşleme başlama zamanını kaydet
            processingJobs.put(jobId, System.currentTimeMillis());
            
            log.info("{} processing job {} for URL: {}", workerName, jobId, url);
            
            // PROCESSING durumuna güncelle
            updateJobStatus(jobId, "PROCESSING", null);
            
            // Burada gerçek Lighthouse analiz işlemi yapılır
            // Simüle edelim ve bazı sonuçlar oluşturalım
            Thread.sleep(10000); // Lighthouse işlemini simüle et
            
            Map<String, Object> results = simulateLighthouseResults(url);
            
            // Sonuçları Redis'e kaydet
            saveJobResults(jobId, results)
                .doOnSuccess(saved -> {
                    if (Boolean.TRUE.equals(saved)) {
                        log.info("{} completed job {} successfully", workerName, jobId);
                    } else {
                        log.warn("{} failed to save results for job {}", workerName, jobId);
                        updateJobStatus(jobId, "FAILED", "Failed to save results");
                    }
                })
                .doOnError(error -> {
                    log.error("{} error saving results for job {}: {}", 
                        workerName, jobId, error.getMessage(), error);
                    updateJobStatus(jobId, "FAILED", "Error saving results: " + error.getMessage());
                })
                .block();
                
            // İşlem tamamlandı, kaydı temizle
            processingJobs.remove(jobId);
            
        } catch (Exception e) {
            log.error("{} error processing job {}: {}", workerName, jobId, e.getMessage(), e);
            updateJobStatus(jobId, "FAILED", "Processing error: " + e.getMessage());
            processingJobs.remove(jobId);
        }
    }
    
    private Mono<Boolean> updateJobStatus(String jobId, String status, String errorMessage) {
        log.debug("Updating job {} status to: {}", jobId, status);
        
        JobStatusResponse jobStatus = JobStatusResponse.builder()
            .jobId(jobId)
            .complete("COMPLETED".equals(status) || "FAILED".equals(status))
            .status(status)
            .error(errorMessage)
            .build();
            
        return redisTemplate.opsForValue().set(resultsPrefix + "status:" + jobId, jobStatus);
    }
    
    private Mono<Boolean> saveJobResults(String jobId, Map<String, Object> results) {
        // Önce job sonuçlarını kaydet
        return redisTemplate.opsForValue().set(resultsPrefix + jobId, results, Duration.ofDays(1))
            .then(updateJobStatus(jobId, "COMPLETED", null));
    }
    
    // Bu yalnızca test amaçlı bir metot, gerçek implementasyonda Lighthouse analiz sonuçları kullanılır
    private Map<String, Object> simulateLighthouseResults(String url) {
        // URL'yi analiz etmek yerine rastgele sonuçlar üret
        double performanceScore = Math.random() * 0.5 + 0.5; // 0.5-1.0 arası rastgele bir değer
        
        return Map.of(
            "id", java.util.UUID.randomUUID().toString(),
            "url", url,
            "timestamp", System.currentTimeMillis(),
            "performance", performanceScore,
            "categories", Map.of(
                "performance", Map.of("score", performanceScore),
                "accessibility", Map.of("score", Math.random() * 0.3 + 0.7),
                "best-practices", Map.of("score", Math.random() * 0.2 + 0.8),
                "seo", Map.of("score", Math.random() * 0.1 + 0.9)
            ),
            "audits", Map.of(
                "first-contentful-paint", Map.of(
                    "score", Math.random() * 0.4 + 0.6,
                    "displayValue", String.format("%.1f s", Math.random() * 2 + 1),
                    "description", "First Contentful Paint marks the time at which the first text or image is painted"
                ),
                "largest-contentful-paint", Map.of(
                    "score", Math.random() * 0.5 + 0.5,
                    "displayValue", String.format("%.1f s", Math.random() * 3 + 2),
                    "description", "Largest Contentful Paint marks the time at which the largest text or image is painted"
                )
            )
        );
    }
    
    public int getActiveWorkerCount() {
        return activeWorkers.get();
    }
    
    @Scheduled(fixedRate = 60000) // Dakikada bir çalış
    public void checkStuckJobs() {
        final long stuckThresholdMs = 5 * 60 * 1000; // 5 dakika
        final long now = System.currentTimeMillis();
        
        processingJobs.forEach((jobId, startTime) -> {
            if (now - startTime > stuckThresholdMs) {
                log.warn("Job {} appears to be stuck (processing for {} ms)", jobId, now - startTime);
                // İsteğe bağlı olarak takılan işleri güncelleyebiliriz
                updateJobStatus(jobId, "FAILED", "Job timed out after " + (now - startTime) / 1000 + " seconds")
                    .doOnSuccess(updated -> {
                        if (Boolean.TRUE.equals(updated)) {
                            log.info("Marked stuck job {} as failed", jobId);
                            processingJobs.remove(jobId);
                        }
                    })
                    .subscribe();
            }
        });
    }
}
