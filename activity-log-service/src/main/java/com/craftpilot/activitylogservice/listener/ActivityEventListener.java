package com.craftpilot.activitylogservice.listener;

import com.craftpilot.activitylogservice.model.ActivityEvent;
import com.craftpilot.activitylogservice.service.ActivityLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
public class ActivityEventListener {

    private final KafkaReceiver<String, ActivityEvent> receiver;
    private final ActivityLogService activityLogService;

    @Value("${kafka.consumer.concurrency:3}")
    private int concurrency;

    public ActivityEventListener(KafkaReceiver<String, ActivityEvent> receiver, ActivityLogService activityLogService) {
        this.receiver = receiver;
        this.activityLogService = activityLogService;
    }

    @EventListener(ApplicationStartedEvent.class)
    public void startKafkaConsumer() {
        log.info("Starting Kafka consumer with concurrency: {}", concurrency);
        
        receiver.receive()
            .doOnNext(record -> log.debug("Received activity event: key={}, topic={}, partition={}, offset={}",
                record.key(), record.topic(), record.partition(), record.offset()))
            .flatMap(this::processRecord, concurrency)
            .doOnError(error -> log.error("Error processing Kafka record: {}", error.getMessage()))
            .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                    .maxBackoff(Duration.ofSeconds(10))
                    .jitter(0.5))
            .onErrorResume(error -> {
                log.error("Error processing Kafka event: {}", error.getMessage(), error);
                return Mono.empty();
            })
            .subscribe();
    }

    private Flux<ReceiverRecord<String, ActivityEvent>> processRecord(ReceiverRecord<String, ActivityEvent> record) {
        ActivityEvent event = record.value();
        
        if (event == null) {
            log.warn("Received null event from Kafka, acknowledging and skipping.");
            record.receiverOffset().acknowledge();
            return Flux.empty();
        }
        
        return Flux.from(activityLogService.processEvent(event)
            .doOnSuccess(result -> {
                log.debug("Successfully processed activity event: {}", event);
                record.receiverOffset().acknowledge();
            })
            .doOnError(error -> {
                log.error("Failed to process activity event {}: {}", event, error.getMessage());
                record.receiverOffset().acknowledge(); // Acknowledge even on error to not block the flow
            })
            .onErrorResume(e -> Mono.empty())
            .thenReturn(record));
    }
}
