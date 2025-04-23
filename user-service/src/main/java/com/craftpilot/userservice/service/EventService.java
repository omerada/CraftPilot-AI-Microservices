package com.craftpilot.userservice.service;

import com.craftpilot.userservice.model.UserPreference;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class EventService {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    @Value("${spring.kafka.enabled:true}")
    private boolean kafkaEnabled;
    
    @Value("${kafka.topic.user-preferences:user-preferences}")
    private String userPreferencesTopic;
    
    @CircuitBreaker(name = "kafkaPublisher", fallbackMethod = "publishEventFallback")
    public Mono<Void> publishPreferenceChangedEvent(UserPreference userPreference) {
        if (!kafkaEnabled || kafkaTemplate == null) {
            log.info("Kafka devre dışı bırakıldı veya kullanılamıyor, event yayınlanmadı: userId={}", 
                    userPreference.getUserId());
            return Mono.empty();
        }
        
        return Mono.fromCallable(() -> {
            try {
                CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(
                        userPreferencesTopic, 
                        userPreference.getUserId(), 
                        userPreference);
                
                return future.orTimeout(3, TimeUnit.SECONDS)
                        .thenApply(result -> {
                            log.debug("Tercih değişikliği olayı başarıyla yayınlandı: userId={}, topic={}, partition={}, offset={}", 
                                    userPreference.getUserId(), 
                                    result.getRecordMetadata().topic(),
                                    result.getRecordMetadata().partition(), 
                                    result.getRecordMetadata().offset());
                            return result;
                        });
            } catch (Exception e) {
                log.warn("Tercih değişikliği olayı yayınlanamadı: userId={}, error={}", 
                        userPreference.getUserId(), e.getMessage());
                throw e;
            }
        })
        .then()
        .onErrorResume(e -> {
            log.warn("Tercih değişikliği olayı yayınlanırken hata oluştu (zarif şekilde işlem devam ediyor): userId={}, error={}", 
                    userPreference.getUserId(), e.getMessage());
            return Mono.empty();
        });
    }
    
    public Mono<Void> publishEventFallback(UserPreference userPreference, Exception e) {
        log.warn("Kafka devre kesici devreye girdi. Tercih değişikliği olayı yayınlanmadı: userId={}, error={}", 
                userPreference.getUserId(), e.getMessage());
        return Mono.empty();
    }
}
