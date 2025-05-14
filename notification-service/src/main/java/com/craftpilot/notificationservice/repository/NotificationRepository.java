package com.craftpilot.notificationservice.repository;

import com.craftpilot.notificationservice.model.Notification;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.time.LocalDateTime;

@Repository
public interface NotificationRepository extends ReactiveMongoRepository<Notification, String> {
 
    Flux<Notification> findByUserIdAndDeletedIsFalse(String userId);

    Flux<Notification> findByUserId(String userId);

    Flux<Notification> findByUserIdAndRead(String userId, boolean read);

    Flux<Notification> findByScheduledAtAfterAndDeletedIsFalseOrderByScheduledAtAsc(Instant time);

    Flux<Notification> findByScheduledAtAfter(LocalDateTime dateTime);

    Flux<Notification> findByScheduledTimeAfter(LocalDateTime dateTime);
}