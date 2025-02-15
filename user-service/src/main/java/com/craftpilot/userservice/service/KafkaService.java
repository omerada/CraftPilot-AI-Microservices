package com.craftpilot.userservice.service;

import com.craftpilot.userservice.model.user.entity.UserEntity;
import com.craftpilot.userservice.model.user.event.UserEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class KafkaService {
    private final KafkaTemplate<String, UserEvent> kafkaTemplate;
    
    @Value("${kafka.topics.user-events}")
    private String userEventsTopic;

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
        kafkaTemplate.send(userEventsTopic, event.getUserId(), event)
            .whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to send user event for ID: {}", event.getUserId(), ex);
                } else {
                    log.debug("User event sent successfully for ID: {}", event.getUserId());
                }
            });
    }
}