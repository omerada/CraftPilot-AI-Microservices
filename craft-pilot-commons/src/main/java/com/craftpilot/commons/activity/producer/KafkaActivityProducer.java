package com.craftpilot.commons.activity.producer;

import com.craftpilot.commons.activity.model.ActivityEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import reactor.core.publisher.Mono;

@Slf4j
public class KafkaActivityProducer implements ActivityProducer {
    
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String topic;
    
    public KafkaActivityProducer(KafkaTemplate<String, Object> kafkaTemplate, String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }
    
    @Override
    public Mono<Void> sendEvent(ActivityEvent event) {
        if (!event.isValid()) {
            log.warn("Invalid activity event: {}", event);
            return Mono.error(new IllegalArgumentException("Invalid activity event"));
        }
        
        return Mono.fromRunnable(() -> {
            String key = event.getUserId() + "-" + System.currentTimeMillis();
            log.debug("Sending activity event to topic {}: {}", topic, event);
            
            kafkaTemplate.send(topic, key, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to send activity event: {}", ex.getMessage());
                    } else {
                        log.debug("Activity event sent successfully");
                    }
                });
        });
    }
}
