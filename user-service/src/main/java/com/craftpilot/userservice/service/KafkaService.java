package com.craftpilot.userservice.service;

import com.craftpilot.userservice.event.UserEvent;
import com.craftpilot.userservice.model.user.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaService {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    private static final String USER_TOPIC = "user-events";
    private static final String USER_CREATED = "USER_CREATED";
    private static final String USER_UPDATED = "USER_UPDATED";
    private static final String USER_DELETED = "USER_DELETED";
    private static final String USER_STATUS_CHANGED = "USER_STATUS_CHANGED";

    public Mono<Void> sendUserCreatedEvent(UserEntity user) {
        return sendUserEvent(USER_CREATED, user);
    }

    public Mono<Void> sendUserUpdatedEvent(UserEntity user) {
        return sendUserEvent(USER_UPDATED, user);
    }

    public Mono<Void> sendUserDeletedEvent(String userId) {
        UserEvent event = UserEvent.builder()
                .eventType(USER_DELETED)
                .userId(userId)
                .timestamp(System.currentTimeMillis())
                .build();
        return sendEvent(userId, event);
    }

    public Mono<Void> sendUserStatusChangedEvent(UserEntity user) {
        return sendUserEvent(USER_STATUS_CHANGED, user);
    }

    private Mono<Void> sendUserEvent(String eventType, UserEntity user) {
        UserEvent event = UserEvent.fromUser(eventType, user);
        return sendEvent(user.getId(), event);
    }

    private Mono<Void> sendEvent(String key, UserEvent event) {
        return Mono.fromFuture(() -> {
            CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(USER_TOPIC, key, event)
                .toCompletableFuture();
            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.debug("Sent event: {} with key: {}, offset: {}", 
                        event, key, result.getRecordMetadata().offset());
                } else {
                    log.error("Failed to send event: {} with key: {}", event, key, ex);
                }
            });
            return future;
        }).then();
    }
} 