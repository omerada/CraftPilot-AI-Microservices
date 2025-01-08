package com.craftpilot.subscriptionservice.repository;

import com.craftpilot.subscriptionservice.model.subscription.entity.UserSubscriptionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserSubscriptionRepository extends JpaRepository<UserSubscriptionEntity, Long> {

    Optional<UserSubscriptionEntity> findByUserId(Long userId);
}

