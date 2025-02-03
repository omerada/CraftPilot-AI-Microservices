package com.craftpilot.subscriptionservice.service;

import com.craftpilot.subscriptionservice.model.subscription.entity.SubscriptionPlan;
import com.craftpilot.subscriptionservice.repository.SubscriptionPlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionPlanService {

    private final SubscriptionPlanRepository planRepository;

    public Mono<SubscriptionPlan> createPlan(SubscriptionPlan plan) {
        plan.setCreatedAt(LocalDateTime.now());
        plan.setUpdatedAt(LocalDateTime.now());
        return planRepository.save(plan);
    }

    public Mono<SubscriptionPlan> updatePlan(String id, SubscriptionPlan plan) {
        return planRepository.findById(id)
                .switchIfEmpty(Mono.error(new RuntimeException("Plan bulunamadı")))
                .flatMap(existingPlan -> {
                    plan.setId(id);
                    plan.setUpdatedAt(LocalDateTime.now());
                    return planRepository.save(plan);
                });
    }

    public Flux<SubscriptionPlan> getAllActivePlans() {
        return planRepository.findAllActive();
    }

    public Mono<SubscriptionPlan> getPlanById(String id) {
        return planRepository.findById(id)
                .switchIfEmpty(Mono.error(new RuntimeException("Plan bulunamadı")));
    }

    public Mono<Void> deletePlan(String id) {
        return planRepository.findById(id)
                .switchIfEmpty(Mono.error(new RuntimeException("Plan bulunamadı")))
                .flatMap(plan -> {
                    plan.setIsDeleted(true);
                    plan.setUpdatedAt(LocalDateTime.now());
                    return planRepository.save(plan).then();
                });
    }

    public Mono<SubscriptionPlan> activatePlan(String id) {
        return planRepository.findById(id)
                .switchIfEmpty(Mono.error(new RuntimeException("Plan bulunamadı")))
                .flatMap(plan -> {
                    plan.setIsActive(true);
                    plan.setUpdatedAt(LocalDateTime.now());
                    return planRepository.save(plan);
                });
    }

    public Mono<SubscriptionPlan> deactivatePlan(String id) {
        return planRepository.findById(id)
                .switchIfEmpty(Mono.error(new RuntimeException("Plan bulunamadı")))
                .flatMap(plan -> {
                    plan.setIsActive(false);
                    plan.setUpdatedAt(LocalDateTime.now());
                    return planRepository.save(plan);
                });
    }
} 