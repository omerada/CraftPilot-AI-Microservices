package com.craftpilot.subscriptionservice.model.subscription.event;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class SubscriptionEventProducer {

    private final KafkaTemplate<String, SubscriptionCreatedEvent> kafkaTemplate;

    @Autowired
    public SubscriptionEventProducer(KafkaTemplate<String, SubscriptionCreatedEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendSubscriptionCreatedEvent(SubscriptionCreatedEvent event) {
        // Kafka üzerinden event'i gönderme
        kafkaTemplate.send("subscription-topic", event);
    }
}
