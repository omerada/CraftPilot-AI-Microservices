package com.craftpilot.contentservice.service;

import com.craftpilot.contentservice.model.Content;
import com.craftpilot.contentservice.event.ContentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaEventPublisherService implements EventPublisherService {

    private final KafkaTemplate<String, ContentEvent> kafkaTemplate;
    private static final String CONTENT_EVENTS_TOPIC = "content-events";

    @Override
    public void publishContentCreated(Content content) {
        ContentEvent event = createContentEvent("CREATED", content);
        kafkaTemplate.send(CONTENT_EVENTS_TOPIC, event.getEventId(), event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Content created event published successfully for content: {}", content.getId());
                    } else {
                        log.error("Error publishing content created event for content: {}", content.getId(), ex);
                    }
                });
    }

    @Override
    public void publishContentUpdated(Content content) {
        ContentEvent event = createContentEvent("UPDATED", content);
        kafkaTemplate.send(CONTENT_EVENTS_TOPIC, event.getEventId(), event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Content updated event published successfully for content: {}", content.getId());
                    } else {
                        log.error("Error publishing content updated event for content: {}", content.getId(), ex);
                    }
                });
    }

    @Override
    public void publishContentDeleted(String id, String userId) {
        ContentEvent event = ContentEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("DELETED")
                .userId(userId)
                .timestamp(Instant.now())
                .build();
        
        kafkaTemplate.send(CONTENT_EVENTS_TOPIC, event.getEventId(), event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Content deleted event published successfully for content: {}", id);
                    } else {
                        log.error("Error publishing content deleted event for content: {}", id, ex);
                    }
                });
    }

    @Override
    public void publishContentPublished(Content content) {
        ContentEvent event = createContentEvent("PUBLISHED", content);
        kafkaTemplate.send(CONTENT_EVENTS_TOPIC, event.getEventId(), event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Content published event published successfully for content: {}", content.getId());
                    } else {
                        log.error("Error publishing content published event for content: {}", content.getId(), ex);
                    }
                });
    }

    @Override
    public void publishContentArchived(Content content) {
        ContentEvent event = createContentEvent("ARCHIVED", content);
        kafkaTemplate.send(CONTENT_EVENTS_TOPIC, event.getEventId(), event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Content archived event published successfully for content: {}", content.getId());
                    } else {
                        log.error("Error publishing content archived event for content: {}", content.getId(), ex);
                    }
                });
    }

    private ContentEvent createContentEvent(String eventType, Content content) {
        return ContentEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(eventType)
                .content(content)
                .userId(content.getUserId())
                .timestamp(Instant.now())
                .build();
    }
} 
