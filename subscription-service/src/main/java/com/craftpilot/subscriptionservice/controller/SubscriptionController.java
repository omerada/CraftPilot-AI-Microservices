package com.craftpilot.subscriptionservice.controller;

import com.craftpilot.subscriptionservice.model.subscription.entity.Subscription;
import com.craftpilot.subscriptionservice.model.subscription.request.CreateSubscriptionRequest;
import com.craftpilot.subscriptionservice.service.SubscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/subscriptions")
@RequiredArgsConstructor
@Tag(name = "Subscription", description = "Subscription management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @PostMapping
    @Operation(summary = "Create a new subscription")
    public Mono<ResponseEntity<Subscription>> createSubscription(@RequestBody CreateSubscriptionRequest request) {
        return subscriptionService.createSubscription(request)
                .map(ResponseEntity::ok);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get subscription by ID")
    public Mono<ResponseEntity<Subscription>> getSubscription(@PathVariable String id) {
        return subscriptionService.getSubscription(id)
                .map(ResponseEntity::ok);
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get all subscriptions for a user")
    public Flux<Subscription> getUserSubscriptions(@PathVariable String userId) {
        return subscriptionService.getUserSubscriptions(userId);
    }

    @GetMapping("/user/{userId}/active")
    @Operation(summary = "Get active subscription for a user")
    public Mono<ResponseEntity<Subscription>> getActiveSubscription(@PathVariable String userId) {
        return subscriptionService.getActiveSubscription(userId)
                .map(ResponseEntity::ok);
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel a subscription")
    public Mono<ResponseEntity<Subscription>> cancelSubscription(@PathVariable String id) {
        return subscriptionService.cancelSubscription(id)
                .map(ResponseEntity::ok);
    }

    @PostMapping("/{id}/activate")
    @Operation(summary = "Activate a subscription")
    public Mono<ResponseEntity<Subscription>> activateSubscription(@PathVariable String id) {
        return subscriptionService.activateSubscription(id)
                .map(ResponseEntity::ok);
    }

    @GetMapping("/expiring")
    @Operation(summary = "Get all expiring subscriptions")
    public Flux<Subscription> getExpiringSubscriptions() {
        return subscriptionService.getExpiringSubscriptions();
    }
} 