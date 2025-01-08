package com.craftpilot.userservice.model.user.event;

public interface EventPublisher {

    /**
     * Publishes an event to the event bus.
     *
     * @param event the event to publish
     */
    void publishEvent(Object event);
}
