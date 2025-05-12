package com.craftpilot.activitylogservice.listener;

import com.craftpilot.commons.activity.model.ActivityEvent;
import com.craftpilot.activitylogservice.service.ActivityLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverRecord;
import reactor.util.retry.Retry;

import java.lang.reflect.InaccessibleObjectException;
import java.time.Duration;

@Component
@Slf4j
@ConditionalOnProperty(name = "spring.kafka.bootstrap-servers")
public class ActivityEventListener {
    
    private final KafkaReceiver<String, ActivityEvent> receiver;
    private final ActivityLogService activityLogService;
    
    @Value("${activity.kafka.retry.max-attempts:3}")
    private int maxRetryAttempts;
    
    @Value("${activity.kafka.retry.initial-backoff:1000}")
    private long initialBackoffMillis;
    
    @Value("${activity.kafka.consumer.topic:user-activity}")
    private String activityTopic;
    
    public ActivityEventListener(KafkaReceiver<String, ActivityEvent> receiver, ActivityLogService activityLogService) {
        this.receiver = receiver;
        this.activityLogService = activityLogService;
    }
    
    @EventListener(ApplicationStartedEvent.class)
    public void startListener() {
        log.info("Starting Kafka consumer for activity events on topic: {}", activityTopic);
        
        Retry retrySpec = Retry.backoff(maxRetryAttempts, Duration.ofMillis(initialBackoffMillis))
                .filter(throwable -> !(throwable instanceof InaccessibleObjectException))
                .doBeforeRetry(signal -> log.warn("Retrying Kafka consumer after error: {}", 
                        signal.failure().getMessage()));
        
        receiver.receive()
                .flatMap(this::processRecord)
                .doOnError(error -> {
                    if (error instanceof InaccessibleObjectException) {
                        log.error("Java reflection error - this requires code changes, not retrying: {}", error.getMessage());
                    } else {
                        log.error("Error in Kafka consumer: {}", error.getMessage(), error);
                    }
                })
                .retryWhen(retrySpec)
                .subscribe(
                    null,
                    error -> log.error("Fatal error in Kafka consumer, gave up after retries: {}", 
                        error.getMessage(), error),
                    () -> log.info("Kafka consumer completed (this should not happen normally)")
                );
        
        log.info("Kafka consumer started successfully and listening for activity events");
    }
    
    private Mono<Void> processRecord(ReceiverRecord<String, ActivityEvent> record) {
        ActivityEvent commonsEvent = record.value();
        
        if (commonsEvent == null) {
            log.warn("Received null activity event, acknowledging and skipping");
            return Mono.fromRunnable(record::receiverOffset).then();
        }
        
        if (!commonsEvent.isValid()) {
            log.warn("Received invalid activity event: {}, acknowledging and skipping", commonsEvent);
            return Mono.fromRunnable(record::receiverOffset).then();
        }
        
        log.debug("Processing activity event: {}", commonsEvent);
        
        // Convert from commons ActivityEvent to service ActivityEvent
        com.craftpilot.activitylogservice.model.ActivityEvent serviceEvent = 
            convertToServiceActivityEvent(commonsEvent);
        
        return activityLogService.processEvent(serviceEvent)
                .doOnSuccess(v -> {
                    record.receiverOffset().acknowledge();
                    log.debug("Successfully processed and acknowledged activity event");
                })
                .doOnError(e -> log.error("Failed to process activity event: {}", e.getMessage(), e))
                .then();
    }
    
    /**
     * Converts a commons ActivityEvent to the service-specific ActivityEvent
     */
    private com.craftpilot.activitylogservice.model.ActivityEvent convertToServiceActivityEvent(
            com.craftpilot.commons.activity.model.ActivityEvent commonsEvent) {
        
        return com.craftpilot.activitylogservice.model.ActivityEvent.builder()
                .userId(commonsEvent.getUserId())
                .timestamp(commonsEvent.getTimestamp())
                .actionType(commonsEvent.getActionType())
                .metadata(commonsEvent.getMetadata())
                .build();
    }
}
