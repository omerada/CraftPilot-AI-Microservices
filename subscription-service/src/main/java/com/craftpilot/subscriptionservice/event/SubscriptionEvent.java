package com.craftpilot.subscriptionservice.event;

import com.craftpilot.subscriptionservice.model.subscription.entity.Subscription;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionEvent {
    private String eventType;
    private String subscriptionId;
    private String userId;
    private Long timestamp;
    private Subscription subscription;
    
    public static SubscriptionEvent fromSubscription(String eventType, Subscription subscription) {
        return SubscriptionEvent.builder()
                .eventType(eventType)
                .subscriptionId(subscription.getId())
                .userId(subscription.getUserId())
                .timestamp(System.currentTimeMillis())
                .subscription(subscription)
                .build();
    }
} 