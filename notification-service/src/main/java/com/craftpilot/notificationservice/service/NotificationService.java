package com.craftpilot.notificationservice.service;

import com.craftpilot.notificationservice.dto.NotificationRequest;
import com.craftpilot.notificationservice.dto.NotificationResponse;
import com.craftpilot.notificationservice.event.NotificationEvent;
import com.craftpilot.notificationservice.model.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationService {
    private final KafkaTemplate<String, NotificationEvent> kafkaTemplate;
    
    @Value("${kafka.topics.notification-events}")
    private String notificationEventsTopic;

    public Mono<Notification> sendNotification(Notification notification) {
        return Mono.just(notification)
            .map(n -> {
                sendNotificationEvent(n, "NOTIFICATION_SENT");
                return n;
            });
    }

    private void sendNotificationEvent(Notification notification, String eventType) {
        NotificationEvent event = NotificationEvent.fromNotification(eventType, notification);
        
        kafkaTemplate.send(notificationEventsTopic, notification.getId(), event)
            .addCallback(
                result -> log.debug("Notification event sent successfully for ID: {}", notification.getId()),
                ex -> log.error("Failed to send notification event for ID: {}", notification.getId(), ex)
            );
    }

    public Mono<NotificationResponse> createNotification(NotificationRequest request) {
        // implementation
    }

    public Mono<NotificationResponse> getNotification(String id) {
        // implementation
    }

    public Flux<NotificationResponse> getUserNotifications(String userId, boolean onlyUnread) {
        // implementation
    }

    public Mono<NotificationResponse> markAsRead(String id) {
        // implementation
    }

    public Mono<Void> deleteNotification(String id) {
        // implementation
    }

    public Flux<Notification> getScheduledNotifications() {
        // implementation
    }

    public Mono<Void> markAsSent(String id) {
        // implementation
    }

    public Mono<Notification> saveNotification(Notification notification) {
        // implementation
    }
}