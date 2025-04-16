package com.craftpilot.lighthouseservice.service;

import com.craftpilot.lighthouseservice.model.JobStatusResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class LighthouseQueueService {
    // Manual logger for fallback
    private static final Logger logger = LoggerFactory.getLogger(LighthouseQueueService.class);
    
    private final ReactiveRedisTemplate<String, Object> redisTemplate;

    @Value("${lighthouse.queue.name}")
    private String queueName;

    @Value("${lighthouse.results.prefix}")
    private String resultsPrefix;

    @Value("${lighthouse.job.timeout:300}")
    private int jobTimeoutSeconds;
    
    public LighthouseQueueService(ReactiveRedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

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
        Map<String, Object> initialStatus = new HashMap<>();
        initialStatus.put("jobId", jobId);
        initialStatus.put("complete", false);
        initialStatus.put("status", "PENDING");
        initialStatus.put("timestamp", System.currentTimeMillis());
            
        logger.info("Job queued with ID: {} for URL: {}", jobId, url);
        
        return redisTemplate.opsForList().rightPush(queueName, job)
                .then(redisTemplate.opsForValue().set(resultsPrefix + "status:" + jobId, initialStatus, Duration.ofMinutes(30)))
                // Job timeout işlemcisini ekle
                .then(redisTemplate.opsForValue().set(resultsPrefix + "timeout:" + jobId, System.currentTimeMillis(), Duration.ofSeconds(jobTimeoutSeconds)))
                .thenReturn(jobId);
    }

    @SuppressWarnings("unchecked")
    public Mono<JobStatusResponse> getJobStatus(String jobId) {
        if (jobId == null || jobId.isEmpty()) {
            logger.error("Invalid job ID: {}", jobId);
            return Mono.error(new IllegalArgumentException("Job ID cannot be null or empty"));
        }
        
        // Önce timeout durumunu kontrol et
        return redisTemplate.opsForValue().get(resultsPrefix + "timeout:" + jobId)
            .flatMap(timeout -> {
                Long startTime = (Long) timeout;
                long currentTime = System.currentTimeMillis();
                
                // Eğer job timeout süresini aştıysa ve hala tamamlanmadıysa
                if (currentTime - startTime > jobTimeoutSeconds * 1000) {
                    logger.warn("Job {} timed out after {} seconds", jobId, jobTimeoutSeconds);
                    
                    // Zaman aşımına uğramış job durumunu güncelle
                    Map<String, Object> timeoutStatus = new HashMap<>();
                    timeoutStatus.put("jobId", jobId);
                    timeoutStatus.put("complete", true);
                    timeoutStatus.put("status", "FAILED");
                    timeoutStatus.put("error", "Job timed out after " + jobTimeoutSeconds + " seconds");
                    timeoutStatus.put("timestamp", currentTime);
                    
                    // Durumu Redis'e kaydet ve yanıt olarak döndür
                    return redisTemplate.opsForValue().set(resultsPrefix + "status:" + jobId, timeoutStatus)
                        .thenReturn(convertMapToJobStatusResponse(timeoutStatus));
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
                logger.debug("Job result found for ID {}: {}", jobId, result);
                // Sonuç içinde hata kontrolü yap
                Map<String, Object> resultMap = (Map<String, Object>) result;
                boolean hasError = resultMap != null && resultMap.containsKey("error") && resultMap.get("error") != null;
                
                JobStatusResponse response = new JobStatusResponse();
                response.setJobId(jobId);
                response.setComplete(true);
                response.setStatus(hasError ? "FAILED" : "COMPLETED");
                if (hasError) {
                    response.setError((String) resultMap.get("error"));
                }
                response.setData(resultMap);
                response.setTimestamp(System.currentTimeMillis());
                return Mono.just(response);
            })
            .switchIfEmpty(Mono.defer(() -> 
                redisTemplate.opsForValue().get(resultsPrefix + "status:" + jobId)
                    .flatMap(obj -> {
                        // Deserialize etmek için doğru tip kontrolü yap
                        if (obj instanceof JobStatusResponse) {
                            return Mono.just((JobStatusResponse) obj);
                        } else if (obj instanceof Map) {
                            // Map'ten JobStatusResponse'a dönüştür
                            return Mono.just(convertMapToJobStatusResponse((Map<String, Object>) obj));
                        } else {
                            logger.warn("Unexpected object type returned from Redis for job {}: {}", 
                                jobId, obj != null ? obj.getClass() : "null");
                            
                            JobStatusResponse response = new JobStatusResponse();
                            response.setJobId(jobId);
                            response.setComplete(false);
                            response.setStatus("UNKNOWN");
                            response.setError("Invalid response format");
                            response.setTimestamp(System.currentTimeMillis());
                            return Mono.just(response);
                        }
                    })
                    .switchIfEmpty(Mono.defer(() -> {
                        JobStatusResponse response = new JobStatusResponse();
                        response.setJobId(jobId);
                        response.setComplete(false);
                        response.setStatus("NOT_FOUND");
                        response.setError("Job not found or expired");
                        response.setTimestamp(System.currentTimeMillis());
                        return Mono.just(response);
                    }))
            ));
    }

    /**
     * Map formatındaki nesneyi JobStatusResponse nesnesine dönüştürür
     */
    private JobStatusResponse convertMapToJobStatusResponse(Map<String, Object> map) {
        if (map == null) {
            logger.warn("Cannot convert null map to JobStatusResponse");
            JobStatusResponse response = new JobStatusResponse();
            response.setStatus("ERROR");
            response.setError("Invalid null response");
            response.setComplete(false);
            response.setTimestamp(System.currentTimeMillis());
            return response;
        }

        JobStatusResponse response = new JobStatusResponse();
        
        // Map'ten değerleri JobStatusResponse nesnesine aktar
        if (map.containsKey("jobId")) response.setJobId((String) map.get("jobId"));
        if (map.containsKey("complete")) response.setComplete((Boolean) map.get("complete"));
        if (map.containsKey("status")) response.setStatus((String) map.get("status"));
        if (map.containsKey("error")) response.setError((String) map.get("error"));
        if (map.containsKey("data") && map.get("data") instanceof Map) {
            response.setData((Map<String, Object>) map.get("data"));
        } else {
            // Eğer data alanı yoksa, tüm map'i data olarak kullan (bazı alanları hariç tut)
            Map<String, Object> dataMap = new HashMap<>(map);
            dataMap.remove("jobId");
            dataMap.remove("complete");
            dataMap.remove("status");
            dataMap.remove("error");
            dataMap.remove("timestamp");
            response.setData(dataMap);
        }
        if (map.containsKey("timestamp")) {
            Object timestamp = map.get("timestamp");
            if (timestamp instanceof Long) {
                response.setTimestamp((Long) timestamp);
            } else if (timestamp instanceof Number) {
                response.setTimestamp(((Number) timestamp).longValue());
            } else if (timestamp instanceof String) {
                try {
                    response.setTimestamp(Long.parseLong((String) timestamp));
                } catch (NumberFormatException e) {
                    response.setTimestamp(System.currentTimeMillis());
                }
            }
        } else {
            response.setTimestamp(System.currentTimeMillis());
        }
        
        return response;
    }
    
    // Job durumunu güncelle (worker tarafından kullanılabilir)
    public Mono<Boolean> updateJobStatus(String jobId, String status, String errorMessage) {
        logger.info("Updating job {} status to: {}", jobId, status);
        
        Map<String, Object> jobStatus = new HashMap<>();
        jobStatus.put("jobId", jobId);
        jobStatus.put("complete", "COMPLETED".equals(status) || "FAILED".equals(status));
        jobStatus.put("status", status);
        jobStatus.put("error", errorMessage);
        jobStatus.put("timestamp", System.currentTimeMillis());
            
        return redisTemplate.opsForValue().set(resultsPrefix + "status:" + jobId, jobStatus);
    }

    // Job durumunu güncelle (worker tarafından kullanılabilir) - data paramı eklenmiş versiyon
    public Mono<Boolean> updateJobStatus(String jobId, String status, String errorMessage, Map<String, Object> data) {
        logger.info("Updating job {} status to: {}", jobId, status);
        
        Map<String, Object> jobStatus = new HashMap<>();
        jobStatus.put("jobId", jobId);
        jobStatus.put("complete", "COMPLETED".equals(status) || "FAILED".equals(status));
        jobStatus.put("status", status);
        jobStatus.put("error", errorMessage);
        jobStatus.put("data", data);
        jobStatus.put("timestamp", System.currentTimeMillis());
            
        return redisTemplate.opsForValue().set(resultsPrefix + "status:" + jobId, jobStatus);
    }
    
    // Redis'teki job sayısını kontrol et
    public Mono<Long> getQueueLength() {
        return redisTemplate.opsForList().size(queueName);
    }
}
