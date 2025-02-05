package com.craftpilot.userservice.service;

import com.craftpilot.userservice.event.UserEvent;
import com.craftpilot.userservice.model.user.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaService {
    private final KafkaTemplate<String, UserEvent> kafkaTemplate;
    private static final String USER_TOPIC = "user-events";

    public Mono<Void> sendUserEvent(String eventType, UserEntity user) {
        return Mono.fromRunnable(() -> {
            UserEvent event = UserEvent.fromUser(eventType, user);
            kafkaTemplate.send(USER_TOPIC, user.getId(), event)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.info("Successfully sent event: {}", event);
                        } else {
                            log.error("Failed to send event: {}", ex.getMessage());
                        }
                    });
        });
    }
} 