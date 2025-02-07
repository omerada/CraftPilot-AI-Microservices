package com.craftpilot.userservice.service;

import com.craftpilot.userservice.event.UserEvent;
import com.craftpilot.userservice.model.user.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service; 

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaService {
    private final KafkaTemplate<String, UserEvent> kafkaTemplate;
    private static final String USER_TOPIC = "user-events";

    public void sendUserCreatedEvent(UserEntity user) {
        sendUserEvent("USER_CREATED", user);
    }

    public void sendUserUpdatedEvent(UserEntity user) {
        sendUserEvent("USER_UPDATED", user);
    }

    public void sendUserDeletedEvent(UserEntity user) {
        sendUserEvent("USER_DELETED", user);
    }

    private void sendUserEvent(String eventType, UserEntity user) {
        UserEvent event = UserEvent.builder()
                .userId(user.getId())
                .eventType(eventType)
                .timestamp(System.currentTimeMillis())
                .build();

        kafkaTemplate.send(USER_TOPIC, user.getId(), event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Sent event {} for user {}", eventType, user.getId());
                    } else {
                        log.error("Failed to send event {} for user {}: {}", 
                                eventType, user.getId(), ex.getMessage());
                    }
                });
    }
} 