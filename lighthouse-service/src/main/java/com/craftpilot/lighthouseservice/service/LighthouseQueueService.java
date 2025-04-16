package com.craftpilot.lighthouseservice.service;

import com.craftpilot.lighthouseservice.model.JobStatusResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class LighthouseQueueService {
    private final ReactiveRedisTemplate<String, Object> redisTemplate;
    
    @Value("${lighthouse.queue.name}")
    private String queueName;
    
    @Value("${lighthouse.results.prefix}")
    private String resultsPrefix;

    @Value("${lighthouse.job.timeout:300}")
    private int jobTimeoutSeconds; // Varsayılan 5 dakika
    
    public Mono<String> queueAnalysisJob(String url, Map<String, Object> options) {
        String jobId = UUID.randomUUID().toString();
        
        Map<String, Object> job = new HashMap<>();
        job.put("id", jobId);
        job.put("url", url);
        job.put("options", options != null ? options : new HashMap<>());
        job.put("timestamp", System.currentTimeMillis());
        job.put("priority", options != null && options.containsKey("priority") ? 
                options.get("priority") : 10); // Öncelik ekle
        
        // Job durumunu PENDING olarak ayarla
        JobStatusResponse initialStatus = JobStatusResponse.builder()
            .jobId(jobId)
            .complete(false)
            .status("PENDING")
            .build();
            
        log.info("Job queued with ID: {} for URL: {}", jobId, url);
        
        return redisTemplate.opsForList().rightPush(queueName, job)
                .then(redisTemplate.opsForValue().set(resultsPrefix + "status:" + jobId, initialStatus, Duration.ofMinutes(30)))
                // Job timeout işlemcisini ekle
                .then(redisTemplate.opsForValue().set(resultsPrefix + "timeout:" + jobId, System.currentTimeMillis(), Duration.ofSeconds(jobTimeoutSeconds)))
                .thenReturn(jobId);
    }
    
    @SuppressWarnings("unchecked")
    public Mono<JobStatusResponse> getJobStatus(String jobId) {
        if (jobId == null || jobId.isEmpty()) {
            log.error("Invalid job ID: {}", jobId);
            return Mono.error(new IllegalArgumentException("Job ID cannot be null or empty"));
        }
        
        // Önce timeout durumunu kontrol et
        return redisTemplate.opsForValue().get(resultsPrefix + "timeout:" + jobId)
            .flatMap(timeout -> {
                Long startTime = (Long) timeout;
                long currentTime = System.currentTimeMillis();
                
                // Eğer job timeout süresini aştıysa ve hala tamamlanmadıysa
                if (currentTime - startTime > jobTimeoutSeconds * 1000) {
                    log.warn("Job {} timed out after {} seconds", jobId, jobTimeoutSeconds);
                    // Zaman aşımına uğramış job durumunu güncelle
                    JobStatusResponse timeoutStatus = JobStatusResponse.builder()
                        .jobId(jobId)
                        .complete(true)
                        .status("FAILED") // Timeout durumunu FAILED olarak değiştirdik
                        .error("Job timed out after " + jobTimeoutSeconds + " seconds")
                        .timestamp(currentTime)
                        .build();
                    
                    // Durumu Redis'e kaydet ve yanıt olarak döndür
                    return redisTemplate.opsForValue().set(resultsPrefix + "status:" + jobId, timeoutStatus)
                        .thenReturn(timeoutStatus);
                }
                
                // Normal akışa devam et
                return processJobStatus(jobId);
            })
            .switchIfEmpty(processJobStatus(jobId)); // Timeout kaydı bulunamazsa normal işleme devam et
    }
    
    @SuppressWarnings("unchecked")
    private Mono<JobStatusResponse> processJobStatus(String jobId) {
        return redisTemplate.opsForValue().get(resultsPrefix + jobId)
            .flatMap(result -> {
                log.debug("Job result found for ID {}: {}", jobId, result);
                // Sonuç içinde hata kontrolü yap
                Map<String, Object> resultMap = (Map<String, Object>) result;
                boolean hasError = resultMap.containsKey("error") && resultMap.get("error") != null;
                
                return Mono.just(JobStatusResponse.builder()
                    .jobId(jobId)
                    .complete(true)
                    .status(hasError ? "FAILED" : "COMPLETED") // Hata durumuna göre FAILED durumu döndür
                    .error(hasError ? (String) resultMap.get("error") : null)
                    .data(resultMap)
                    .timestamp(System.currentTimeMillis())
                    .build());
            })
            .switchIfEmpty(Mono.defer(() -> 
                redisTemplate.opsForValue().get(resultsPrefix + "status:" + jobId)
                    .flatMap(obj -> {
                        // Deserialize etmek için doğru tip kontrolü yap
                        if (obj instanceof JobStatusResponse) {
                            return Mono.just((JobStatusResponse) obj);
                        } else if (obj instanceof Map) {
                            // Map'ten JobStatusResponse'a dönüştür
                            return Mono.just(convertMapToJobStatusResponse((Map<String, Object>) obj, jobId));
                        } else {
                            log.warn("Unexpected object type returned from Redis for job {}: {}", 
                                jobId, obj != null ? obj.getClass() : "null");
                            return Mono.just(JobStatusResponse.builder()
                                .jobId(jobId)
                                .complete(false)
                                .status("UNKNOWN")
                                .error("Invalid response format")
                                .timestamp(System.currentTimeMillis())
                                .build());
                        }
                    })
                    .switchIfEmpty(Mono.just(JobStatusResponse.builder()
                        .jobId(jobId)
                        .complete(false)
                        .status("NOT_FOUND")
                        .error("Job not found or expired")
                        .timestamp(System.currentTimeMillis())
                        .build()))
            ));
    }

    /**
     * Map formatındaki nesneyi JobStatusResponse nesnesine dönüştürür
     */
    @SuppressWarnings("unchecked")
    private JobStatusResponse convertMapToJobStatusResponse(Map<String, Object> map, String jobId) {
        log.debug("Converting Map to JobStatusResponse for job {}: {}", jobId, map);
        
        JobStatusResponse.JobStatusResponseBuilder builder = JobStatusResponse.builder();
        
        // JobId bilgisini ekle
        builder.jobId(jobId);
        
        // Timestamp ekle
        builder.timestamp(System.currentTimeMillis());
        
        // Map'ten değerleri çıkar ve builder'a ekle
        if (map.containsKey("complete")) {
            builder.complete((Boolean) map.get("complete"));
        }
        
        if (map.containsKey("status")) {
            builder.status((String) map.get("status"));
        }
        
        if (map.containsKey("error")) {
            String errorMsg = (String) map.get("error");
            builder.error(errorMsg);
            
            // Eğer hata varsa ve durum COMPLETED ise, FAILED olarak düzelt
            if (errorMsg != null && !errorMsg.isEmpty() && 
                (map.containsKey("status") && "COMPLETED".equals(map.get("status")))) {
                builder.status("FAILED");
            }
        }
        
        if (map.containsKey("data") && map.get("data") instanceof Map) {
            builder.data((Map<String, Object>) map.get("data"));
        }
        
        return builder.build();
    }
    
    // Job durumunu güncelle (worker tarafından kullanılabilir)
    public Mono<Boolean> updateJobStatus(String jobId, String status, String errorMessage) {
        log.info("Updating job {} status to: {}", jobId, status);
        
        JobStatusResponse jobStatus = JobStatusResponse.builder()
            .jobId(jobId)
            .complete("COMPLETED".equals(status) || "FAILED".equals(status))
            .status(status)
            .error(errorMessage)
            .build();
            
        return redisTemplate.opsForValue().set(resultsPrefix + "status:" + jobId, jobStatus);
    }

    // Job durumunu güncelle (worker tarafından kullanılabilir)
    public Mono<Boolean> updateJobStatus(String jobId, String status, String errorMessage, Map<String, Object> data) {
        log.info("Updating job {} status to: {}", jobId, status);
        
        JobStatusResponse jobStatus = JobStatusResponse.builder()
            .jobId(jobId)
            .complete("COMPLETED".equals(status) || "FAILED".equals(status))
            .status(status)
            .error(errorMessage)
            .data(data)
            .build();
            
        return redisTemplate.opsForValue().set(resultsPrefix + "status:" + jobId, jobStatus);
    }
    
    // Redis'teki job sayısını kontrol et
    public Mono<Long> getQueueLength() {
        return redisTemplate.opsForList().size(queueName);
    }
}
