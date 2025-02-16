package com.craftpilot.subscriptionservice.controller;

import com.craftpilot.subscriptionservice.controller.dto.SubscriptionPlanDto;
import com.craftpilot.subscriptionservice.service.SubscriptionPlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/subscription-plans")
@RequiredArgsConstructor
@Tag(name = "Subscription Plans", description = "Subscription Plan management APIs")
@SecurityRequirement(name = "bearerAuth")
public class SubscriptionPlanController {

    private final SubscriptionPlanService planService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new subscription plan")
    public Mono<SubscriptionPlanDto> createPlan(
            @RequestHeader("X-User-Role") String userRole,
            @Valid @RequestBody SubscriptionPlanDto planDto) {
        validateAdminRole(userRole);
        return planService.createPlan(planDto.toEntity())
                .map(SubscriptionPlanDto::fromEntity);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a subscription plan by ID")
    public Mono<SubscriptionPlanDto> getPlan(@PathVariable String id) {
        return planService.getPlanById(id)
                .map(SubscriptionPlanDto::fromEntity);
    }

    @GetMapping
    @Operation(summary = "Get all active subscription plans")
    public Flux<SubscriptionPlanDto> getAllActivePlans() {
        return planService.getAllActivePlans()
                .map(SubscriptionPlanDto::fromEntity);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a subscription plan")
    public Mono<SubscriptionPlanDto> updatePlan(
            @RequestHeader("X-User-Role") String userRole,
            @PathVariable String id,
            @Valid @RequestBody SubscriptionPlanDto planDto) {
        validateAdminRole(userRole);
        return planService.updatePlan(id, planDto.toEntity())
                .map(SubscriptionPlanDto::fromEntity);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a subscription plan")
    public Mono<Void> deletePlan(
            @RequestHeader("X-User-Role") String userRole,
            @PathVariable String id) {
        validateAdminRole(userRole);
        return planService.deletePlan(id);
    }

    @PutMapping("/{id}/activate")
    @Operation(summary = "Activate a subscription plan")
    public Mono<SubscriptionPlanDto> activatePlan(
            @RequestHeader("X-User-Role") String userRole,
            @PathVariable String id) {
        validateAdminRole(userRole);
        return planService.activatePlan(id)
                .map(SubscriptionPlanDto::fromEntity);
    }

    @PutMapping("/{id}/deactivate")
    @Operation(summary = "Deactivate a subscription plan")
    public Mono<SubscriptionPlanDto> deactivatePlan(
            @RequestHeader("X-User-Role") String userRole,
            @PathVariable String id) {
        validateAdminRole(userRole);
        return planService.deactivatePlan(id)
                .map(SubscriptionPlanDto::fromEntity);
    }

    private void validateAdminRole(String userRole) {
        if (!"ADMIN".equals(userRole)) {
            throw new SecurityException("Only admin users can perform this operation");
        }
    }
} 