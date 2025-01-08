package com.craftpilot.subscriptionservice.service.impl;

import com.craftpilot.subscriptionservice.model.auth.SubscriptionStatus;
import com.craftpilot.subscriptionservice.model.subscription.dto.request.SubscriptionRequest;
import com.craftpilot.subscriptionservice.model.subscription.entity.UserSubscriptionEntity;
import com.craftpilot.subscriptionservice.model.subscription.event.SubscriptionCreatedEvent;
import com.craftpilot.subscriptionservice.model.subscription.event.SubscriptionEventProducer;
import com.craftpilot.subscriptionservice.repository.UserSubscriptionRepository;
import com.craftpilot.subscriptionservice.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SubscriptionServiceImpl implements SubscriptionService {

    private final UserSubscriptionRepository userSubscriptionRepository;
    private final SubscriptionEventProducer subscriptionEventProducer;

    @Override
    public UserSubscriptionEntity createSubscription(SubscriptionRequest subscriptionRequest) {
        // Abonelik oluşturma işlemi
        UserSubscriptionEntity subscription = new UserSubscriptionEntity();
        subscription.setUserId(subscriptionRequest.getUserId());
        subscription.setSubscriptionPlanId(subscriptionRequest.getSubscriptionPlanId());
        subscription.setStartDate(subscriptionRequest.getStartDate());
        subscription.setEndDate(subscriptionRequest.getEndDate());
        subscription.setStatus(SubscriptionStatus.ACTIVE);

        // Event tetikle
        SubscriptionCreatedEvent event = new SubscriptionCreatedEvent(subscription);
        subscriptionEventProducer.sendSubscriptionCreatedEvent(event);

        return userSubscriptionRepository.save(subscription);
    }

    @Override
    public UserSubscriptionEntity updateSubscription(Long subscriptionId, SubscriptionRequest subscriptionRequest) {
        // Abonelik güncelleme işlemi
        UserSubscriptionEntity subscription = userSubscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new RuntimeException("Abonelik bulunamadı"));

        subscription.setSubscriptionPlanId(subscriptionRequest.getSubscriptionPlanId());
        subscription.setStartDate(subscriptionRequest.getStartDate());
        subscription.setEndDate(subscriptionRequest.getEndDate());

        return userSubscriptionRepository.save(subscription);
    }

    @Override
    public void cancelSubscription(Long subscriptionId) {
        // Abonelik iptal etme işlemi
        UserSubscriptionEntity subscription = userSubscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new RuntimeException("Abonelik bulunamadı"));

        subscription.setStatus(SubscriptionStatus.CANCELLED);
        userSubscriptionRepository.save(subscription);
    }
}

