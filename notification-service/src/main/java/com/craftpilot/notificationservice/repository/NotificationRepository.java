package com.craftpilot.notificationservice.repository;

import com.craftpilot.notificationservice.model.Notification;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.time.LocalDateTime;

@Repository
public interface NotificationRepository extends ReactiveMongoRepository<Notification, String> {

    Flux<Notification> findByUserIdAndDeletedFalse(String userId);

    Flux<Notification> findByUserIdAndReadAndDeletedFalse(String userId, boolean read);

    Flux<Notification> findByScheduledAtAfterAndDeletedFalseOrderByScheduledAtAsc(Instant time);

    Flux<Notification> findByScheduledAtAfterAndDeletedFalse(LocalDateTime dateTime);
}