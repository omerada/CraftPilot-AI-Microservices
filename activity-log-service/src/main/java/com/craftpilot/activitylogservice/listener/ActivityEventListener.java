package com.craftpilot.activitylogservice.listener;

import com.craftpilot.activitylogservice.model.ActivityEvent;
import com.craftpilot.activitylogservice.service.ActivityLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverRecord;
import reactor.util.retry.Retry;

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
                .doBeforeRetry(signal -> log.warn("Retrying Kafka consumer after error: {}", 
                        signal.failure().getMessage()));
        
        receiver.receive()
                .flatMap(this::processRecord)
                .doOnError(error -> log.error("Error in Kafka consumer: {}", error.getMessage(), error))
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
        log.info("Processing activity event: {}", record.value());
        return activityLogService.processEvent(record.value())
            .doOnSuccess(result -> {
                record.receiverOffset().acknowledge();
                log.info("Successfully processed and acknowledged activity event");
            })
            .doOnError(error -> log.error("Failed to process activity event: {}", error.getMessage(), error))
            .then();
    }
}
