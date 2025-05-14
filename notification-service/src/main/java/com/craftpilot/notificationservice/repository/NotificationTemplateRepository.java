package com.craftpilot.notificationservice.repository;

import com.craftpilot.notificationservice.model.NotificationTemplate;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface NotificationTemplateRepository extends ReactiveMongoRepository<NotificationTemplate, String> {

    Flux<NotificationTemplate> findByActiveAndDeleted(boolean active, boolean deleted);
}