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
                .eventType(eventType)
                .user(user)
                .timestamp(System.currentTimeMillis())
                .build();

        try {
            kafkaTemplate.send(USER_TOPIC, user.getId(), event)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.info("User event sent successfully: {}", event);
                        } else {
                            log.error("Failed to send user event: {}", event, ex);
                        }
                    });
        } catch (Exception e) {
            log.error("Error while sending user event: {}", event, e);
        }
    }
} 