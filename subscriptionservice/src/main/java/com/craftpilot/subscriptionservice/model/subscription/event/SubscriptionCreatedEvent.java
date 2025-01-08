package com.craftpilot.subscriptionservice.model.subscription.event;

import com.craftpilot.subscriptionservice.model.subscription.entity.UserSubscriptionEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SubscriptionCreatedEvent {

    private UserSubscriptionEntity userSubscription;

    public SubscriptionCreatedEvent(UserSubscriptionEntity userSubscription) {
        this.userSubscription = userSubscription;
    }
}
