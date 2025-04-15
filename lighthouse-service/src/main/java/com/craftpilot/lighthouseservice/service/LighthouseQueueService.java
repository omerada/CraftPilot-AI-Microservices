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
    
    public Mono<String> queueAnalysisJob(String url, Map<String, Object> options) {
        String jobId = UUID.randomUUID().toString();
        
        Map<String, Object> job = new HashMap<>();
        job.put("id", jobId);
        job.put("url", url);
        job.put("options", options != null ? options : new HashMap<>());
        job.put("timestamp", System.currentTimeMillis());
        
        // Job durumunu PENDING olarak ayarla
        JobStatusResponse initialStatus = JobStatusResponse.builder()
            .jobId(jobId)
            .complete(false)
            .status("PENDING")
            .build();
            
        log.info("Job queued with ID: {}", jobId);
        
        return redisTemplate.opsForList().rightPush(queueName, job)
                .then(redisTemplate.opsForValue().set(resultsPrefix + "status:" + jobId, initialStatus, Duration.ofHours(1)))
                .thenReturn(jobId);
    }
    
    @SuppressWarnings("unchecked")
    public Mono<JobStatusResponse> getJobStatus(String jobId) {
        if (jobId == null || jobId.isEmpty()) {
            log.error("Invalid job ID: {}", jobId);
            return Mono.error(new IllegalArgumentException("Job ID cannot be null or empty"));
        }
        
        return redisTemplate.opsForValue().get(resultsPrefix + jobId)
            .flatMap(result -> {
                log.debug("Job result found: {}", result);
                return Mono.just(JobStatusResponse.builder()
                    .jobId(jobId)
                    .complete(true)
                    .data((Map<String, Object>) result)
                    .build());
            })
            .switchIfEmpty(Mono.defer(() -> 
                redisTemplate.opsForValue().get(resultsPrefix + "status:" + jobId)
                    .map(obj -> (JobStatusResponse) obj)
                    .switchIfEmpty(Mono.just(JobStatusResponse.builder()
                        .jobId(jobId)
                        .complete(false)
                        .status("UNKNOWN")
                        .build()))
            ));
    }
}
