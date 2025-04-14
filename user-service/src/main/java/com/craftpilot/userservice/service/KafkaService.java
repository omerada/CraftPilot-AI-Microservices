package com.craftpilot.userservice.service;

import com.craftpilot.userservice.model.user.entity.UserEntity;
import com.craftpilot.userservice.model.user.event.UserEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
@Slf4j
@RequiredArgsConstructor
public class KafkaService {
    private final KafkaTemplate<String, UserEvent> kafkaTemplate;
    
    @Value("${kafka.topics.user-events}")
    private String userEventsTopic;
    
    @Value("${kafka.producer.timeout:5}")
    private int kafkaTimeout;
    
    @Value("${kafka.producer.enabled:true}")
    private boolean kafkaEnabled;

    public void sendUserCreatedEvent(UserEntity user) {
        sendUserEvent(UserEvent.fromEntity(user, "USER_CREATED"));
    }

    public void sendUserUpdatedEvent(UserEntity user) {
        sendUserEvent(UserEvent.fromEntity(user, "USER_UPDATED"));
    }

    public void sendUserDeletedEvent(UserEntity user) {
        sendUserEvent(UserEvent.fromEntity(user, "USER_DELETED"));
    }

    private void sendUserEvent(UserEvent event) {
        if (!kafkaEnabled) {
            log.warn("Kafka messaging is disabled, skipping event for ID: {}", event.getUserId());
            return;
        }
        
        try {
            // KafkaTemplate.send() doğrudan CompletableFuture döner
            CompletableFuture<SendResult<String, UserEvent>> future = 
                kafkaTemplate.send(userEventsTopic, event.getUserId(), event);
                
            // Asenkron işlemi timeout ile sınırlayalım
            future.orTimeout(kafkaTimeout, TimeUnit.SECONDS)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        if (ex instanceof TimeoutException) {
                            log.error("Kafka message send timed out for user ID: {}", event.getUserId());
                        } else {
                            log.error("Failed to send user event for ID: {}", event.getUserId(), ex);
                        }
                    } else {
                        log.debug("User event sent successfully for ID: {}", event.getUserId());
                    }
                });
        } catch (Exception e) {
            log.error("Error attempting to send Kafka message for user ID: {}", event.getUserId(), e);
            // Hata durumunda servisin çalışmasını engellememek için exception'ı yutuyoruz
        }
    }
}