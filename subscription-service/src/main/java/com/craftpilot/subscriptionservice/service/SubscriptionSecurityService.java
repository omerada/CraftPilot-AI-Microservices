package com.craftpilot.subscriptionservice.service;

import com.craftpilot.subscriptionservice.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service; 

@Service
@RequiredArgsConstructor
public class SubscriptionSecurityService {
    private final SubscriptionRepository subscriptionRepository;

    public boolean isSubscriptionOwner(String userId, String subscriptionId) {
        return subscriptionRepository.findById(subscriptionId)
                .map(subscription -> subscription.getUserId().equals(userId))
                .defaultIfEmpty(false)
                .block();
    }
}