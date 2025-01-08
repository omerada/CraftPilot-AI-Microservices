package com.craftpilot.subscriptionservice.repository;

import com.craftpilot.subscriptionservice.model.subscription.entity.SubscriptionPlanEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for accessing subscription plan data.
 */
@Repository
public interface SubscriptionPlanRepository extends JpaRepository<SubscriptionPlanEntity, Long> {

    /**
     * Finds a subscription plan by its name.
     *
     * @param name the name of the subscription plan.
     * @return the subscription plan entity.
     */
    SubscriptionPlanEntity findByName(String name);
}

