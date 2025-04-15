package com.craftpilot.lighthouseservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
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
    
    private final AtomicInteger activeWorkers = new AtomicInteger(0);
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final Map<String, Long> processingJobs = new ConcurrentHashMap<>();
    
    @PostConstruct
    public void init() {
        log.info("Initializing Lighthouse worker service with {} workers", workerCount);
        // Worker'ları başlat
        startWorkers();
    }
    
    @PreDestroy
    public void shutdown() {
        log.info("Shutting down Lighthouse worker service");
        running.set(false);
    }
    
    public void startWorkers() {
        // İlk worker'ları başlat
        for (int i = 0; i < workerCount; i++) {
            if (running.get()) {
                startWorker(i);
            }
        }
    }
    
    @Scheduled(fixedDelayString = "${lighthouse.worker.check-interval:10000}")
    public void checkAndRestartWorkers() {
        // Worker sayısını kontrol et ve gerekirse yeni worker'lar başlat
        if (activeWorkers.get() < workerCount && running.get()) {
            log.debug("Active workers: {}/{}, starting more workers", activeWorkers.get(), workerCount);
            for (int i = activeWorkers.get(); i < workerCount; i++) {
                startWorker(i);
            }
        }
    }
    
    private void startWorker(int workerId) {
        final String workerName = "worker-" + workerId;
        
        log.info("Starting Lighthouse worker: {}", workerName);
        activeWorkers.incrementAndGet();
        log.info("{} started, active workers: {}/{}", workerName, activeWorkers.get(), workerCount);
        
        // Sürekli olarak kuyruktan işleri alan ve işleyen bir flux
        Flux.interval(Duration.ofMillis(pollInterval))
            .takeWhile(i -> running.get())
            .flatMap(i -> pollJob(workerName))
            .onErrorContinue((error, obj) -> {
                log.error("{} error in job processing: {}", workerName, error.getMessage(), error);
            })
            .doFinally(signal -> {
                activeWorkers.decrementAndGet();
                log.info("{} stopped, remaining active workers: {}", workerName, activeWorkers.get());
            })
            .subscribeOn(Schedulers.boundedElastic())
            .subscribe();
    }
    
    private Mono<Void> pollJob(String workerName) {
        // LPOP komutunu kullanarak kuyruktan bir iş al
        return redisTemplate.opsForList().leftPop(queueName)
            .timeout(Duration.ofSeconds(3))
            .flatMap(job -> {
                if (job instanceof Map) {
                    try {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> jobMap = (Map<String, Object>) job;
                        String jobId = (String) jobMap.get("id");
                        String url = (String) jobMap.get("url");
                        
                        if (jobId != null && url != null) {
                            log.info("{} received job {} for URL: {}", workerName, jobId, url);
                            return processJob(jobId, url, jobMap, workerName);
                        } else {
                            log.warn("{} received invalid job: {}", workerName, jobMap);
                        }
                    } catch (Exception e) {
                        log.error("{} error processing job: {}", workerName, e.getMessage(), e);
                    }
                } else if (job != null) {
                    log.warn("{} received job of unexpected type: {}", workerName, 
                        job.getClass().getName());
                }
                return Mono.empty();
            })
            .onErrorResume(e -> {
                // TimeoutException'ları sessizce işle, diğer hataları logla
                if (!(e instanceof java.util.concurrent.TimeoutException)) {
                    log.error("{} error polling jobs: {}", workerName, e.getMessage(), e);
                }
                return Mono.empty();
            })
            .then();
    }
    
    private Mono<Void> processJob(String jobId, String url, Map<String, Object> jobMap, String workerName) {
        // İşleme başlama zamanını kaydet
        processingJobs.put(jobId, System.currentTimeMillis());
        
        // İlk olarak job durumunu PROCESSING olarak güncelleyelim
        return updateJobStatus(jobId, "PROCESSING", null)
            .timeout(Duration.ofSeconds(5))
            .flatMap(success -> {
                if (Boolean.TRUE.equals(success)) {
                    log.debug("{} updated job {} status to PROCESSING", workerName, jobId);
                } else {
                    log.warn("{} failed to update job {} status to PROCESSING", workerName, jobId);
                }
                
                // Process işlemi bir Mono<Map<String,Object>> döndürüyor
                return simulateLighthouseAnalysis(url)
                    .timeout(Duration.ofSeconds(30))
                    .flatMap(results -> saveJobResults(jobId, results)
                        .timeout(Duration.ofSeconds(5))
                        .flatMap(saved -> {
                            if (Boolean.TRUE.equals(saved)) {
                                log.info("{} completed job {} successfully", workerName, jobId);
                                return Mono.empty();
                            } else {
                                log.warn("{} failed to save results for job {}", workerName, jobId);
                                return updateJobStatus(jobId, "FAILED", "Failed to save results");
                            }
                        })
                        .onErrorResume(error -> {
                            log.error("{} error saving results for job {}: {}", 
                                workerName, jobId, error.getMessage(), error);
                            return updateJobStatus(jobId, "FAILED", "Error saving results: " + error.getMessage());
                        })
                    )
                    .onErrorResume(error -> {
                        log.error("{} error processing job {}: {}", 
                            workerName, jobId, error.getMessage(), error);
                        return updateJobStatus(jobId, "FAILED", "Processing error: " + error.getMessage());
                    })
                    .doFinally(signal -> processingJobs.remove(jobId));
            })
            .then();
    }
    
    private Mono<Boolean> updateJobStatus(String jobId, String status, String errorMessage) {
        log.debug("Updating job {} status to: {}", jobId, status);
        
        Map<String, Object> jobStatus = Map.of(
            "jobId", jobId,
            "complete", "COMPLETED".equals(status) || "FAILED".equals(status),
            "status", status,
            "error", errorMessage != null ? errorMessage : "",
            "timestamp", System.currentTimeMillis()
        );
            
        return redisTemplate.opsForValue()
            .set(resultsPrefix + "status:" + jobId, jobStatus, Duration.ofMinutes(30))
            .timeout(Duration.ofSeconds(3))
            .onErrorResume(e -> {
                log.error("Failed to update job status: {}", e.getMessage(), e);
                return Mono.just(false);
            });
    }
    
    private Mono<Boolean> saveJobResults(String jobId, Map<String, Object> results) {
        // Önce job sonuçlarını kaydet
        return redisTemplate.opsForValue()
            .set(resultsPrefix + jobId, results, Duration.ofDays(1))
            .then(updateJobStatus(jobId, "COMPLETED", null))
            .timeout(Duration.ofSeconds(5))
            .onErrorResume(e -> {
                log.error("Failed to save job results: {}", e.getMessage(), e);
                return Mono.just(false);
            });
    }
    
    // Lighthouse analiz işlemini simüle eder
    private Mono<Map<String, Object>> simulateLighthouseAnalysis(String url) {
        // Gerçek bir analiz yapmak yerine, sadece gecikmeyi simüle eder ve rastgele sonuçlar üretir
        return Mono.delay(Duration.ofSeconds(3))
            .map(unused -> {
                double performanceScore = Math.random() * 0.5 + 0.5; // 0.5-1.0 arası rastgele bir değer
                
                return Map.of(
                    "id", UUID.randomUUID().toString(),
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
            });
    }
    
    // Aktif çalışan worker sayısını döndürür - sağlık kontrolü için kullanılır
    public int getActiveWorkerCount() {
        return activeWorkers.get();
    }
    
    // Takılmış işleri kontrol et
    @Scheduled(fixedRate = 60000) // her dakika
    public void checkStuckJobs() {
        final long stuckThresholdMs = 5 * 60 * 1000; // 5 dakika
        final long now = System.currentTimeMillis();
        
        processingJobs.forEach((jobId, startTime) -> {
            if (now - startTime > stuckThresholdMs) {
                log.warn("Job {} appears to be stuck (processing for {} ms)", jobId, now - startTime);
                updateJobStatus(jobId, "FAILED", "Job timed out after " + (now - startTime) / 1000 + " seconds")
                    .subscribe(
                        updated -> {
                            if (Boolean.TRUE.equals(updated)) {
                                log.info("Marked stuck job {} as failed", jobId);
                                processingJobs.remove(jobId);
                            }
                        },
                        error -> log.error("Failed to mark job {} as failed: {}", jobId, error.getMessage())
                    );
            }
        });
    }
}
