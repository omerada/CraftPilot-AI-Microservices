package com.craftpilot.commons.activity.producer;

import com.craftpilot.commons.activity.config.ActivityConfiguration;
import com.craftpilot.commons.activity.model.ActivityEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import reactor.core.publisher.Mono;

@Slf4j
public class ActivityProducer {
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ActivityConfiguration config;
    
    public ActivityProducer(KafkaTemplate<String, Object> kafkaTemplate, ActivityConfiguration config) {
        this.kafkaTemplate = kafkaTemplate;
        this.config = config;
    }
    
    /**
     * Aktivite olayını Kafka'ya gönderir
     */
    public Mono<Void> sendEvent(ActivityEvent event) {
        if (!config.isEnabled()) {
            log.debug("Activity logging is disabled");
            return Mono.empty();
        }
        
        if (!event.isValid()) {
            log.warn("Invalid activity event: {}", event);
            return Mono.error(new IllegalArgumentException("Invalid activity event"));
        }
        
        return Mono.fromRunnable(() -> {
            String key = generateEventKey(event);
            log.debug("Sending activity event to topic {}: {}", config.getTopic(), event);
            
            kafkaTemplate.send(config.getTopic(), key, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to send activity event: {}", ex.getMessage());
                        handleError(ex);
                    } else {
                        log.debug("Activity event sent successfully");
                    }
                });
        });
    }
    
    private String generateEventKey(ActivityEvent event) {
        return event.getUserId() + "-" + System.currentTimeMillis();
    }
    
    private void handleError(Throwable error) {
        switch (config.getErrorHandling().toLowerCase()) {
            case "fail":
                throw new RuntimeException("Activity event sending failed", error);
            case "log-only":
                log.error("Activity event sending failed (log-only mode): {}", error.getMessage());
                break;
            case "ignore":
            default:
                // Just ignore and continue
                break;
        }
    }
}
