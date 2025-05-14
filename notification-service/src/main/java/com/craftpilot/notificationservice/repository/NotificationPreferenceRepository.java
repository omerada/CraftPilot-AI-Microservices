package com.craftpilot.notificationservice.repository;

import com.craftpilot.notificationservice.model.NotificationPreference;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface NotificationPreferenceRepository extends ReactiveMongoRepository<NotificationPreference, String> {

    // Fix method name to match the isDeleted() getter
    Mono<NotificationPreference> findByUserIdAndDeletedIsFalse(String userId);

    Mono<NotificationPreference> findByUserId(String userId);

    Mono<Void> deleteByUserId(String userId);
}