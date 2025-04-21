package com.craftpilot.commons.activity.producer;

import com.craftpilot.commons.activity.model.ActivityEvent;
import reactor.core.publisher.Mono;

public interface ActivityProducer {
    Mono<Void> sendEvent(ActivityEvent event);
}
