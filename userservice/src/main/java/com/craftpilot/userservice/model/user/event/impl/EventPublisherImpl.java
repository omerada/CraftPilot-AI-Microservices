package com.craftpilot.userservice.model.user.event.impl;

import com.craftpilot.userservice.model.user.event.EventPublisher;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class EventPublisherImpl implements EventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    public EventPublisherImpl(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public void publishEvent(Object event) {
        applicationEventPublisher.publishEvent(event);
    }

}
