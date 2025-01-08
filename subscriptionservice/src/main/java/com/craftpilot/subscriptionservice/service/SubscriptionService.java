package com.craftpilot.subscriptionservice.service;


import com.craftpilot.subscriptionservice.model.subscription.dto.request.SubscriptionRequest;
import com.craftpilot.subscriptionservice.model.subscription.entity.UserSubscriptionEntity;

public interface SubscriptionService {

    UserSubscriptionEntity createSubscription(SubscriptionRequest subscriptionRequest);

    UserSubscriptionEntity updateSubscription(Long subscriptionId, SubscriptionRequest subscriptionRequest);

    void cancelSubscription(Long subscriptionId);
}

