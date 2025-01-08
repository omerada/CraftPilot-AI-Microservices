package com.craftpilot.subscriptionservice.service;

import com.craftpilot.subscriptionservice.model.subscription.dto.SubscriptionPlan;

import java.util.List;

/**
 * Service interface for managing subscription plans.
 */
public interface SubscriptionPlanService {

    /**
     * Creates a new subscription plan.
     *
     * @param subscriptionPlanDTO the subscription plan details.
     * @return the created subscription plan as DTO.
     */
    SubscriptionPlan createSubscriptionPlan(SubscriptionPlan subscriptionPlanDTO);

    /**
     * Retrieves a subscription plan by its ID.
     *
     * @param id the ID of the subscription plan.
     * @return the subscription plan as DTO.
     */
    SubscriptionPlan getSubscriptionPlanById(Long id);

    /**
     * Updates an existing subscription plan.
     *
     * @param id the ID of the subscription plan to be updated.
     * @param subscriptionPlanDTO the updated subscription plan details.
     * @return the updated subscription plan as DTO.
     */
    SubscriptionPlan updateSubscriptionPlan(Long id, SubscriptionPlan subscriptionPlanDTO);

    /**
     * Deletes a subscription plan by its ID.
     *
     * @param id the ID of the subscription plan to be deleted.
     */
    void deleteSubscriptionPlan(Long id);

    /**
     * Retrieves all subscription plans.
     *
     * @return list of all subscription plans.
     */
    List<SubscriptionPlan> getAllSubscriptionPlans();
}
