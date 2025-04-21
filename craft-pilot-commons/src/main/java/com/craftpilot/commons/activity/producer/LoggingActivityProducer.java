package com.craftpilot.commons.activity.producer;

import com.craftpilot.commons.activity.model.ActivityEvent;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
public class LoggingActivityProducer implements ActivityProducer {
    
    @Override
    public Mono<Void> sendEvent(ActivityEvent event) {
        log.info("Activity event (logging only): userId={}, actionType={}, timestamp={}",
                event.getUserId(), event.getActionType(), event.getTimestamp());
        return Mono.empty();
    }
}
