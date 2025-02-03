package com.craftpilot.subscriptionservice.controller;

import com.craftpilot.subscriptionservice.controller.dto.PaymentRequestDto;
import com.craftpilot.subscriptionservice.model.payment.entity.Payment; 
import com.craftpilot.subscriptionservice.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Payment management APIs")
@SecurityRequirement(name = "bearerAuth")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Process a new payment")
    public Mono<Payment> processPayment(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody PaymentRequestDto paymentRequest) {
        return paymentService.processPayment(userId, paymentRequest);
    }

    @PostMapping("/{id}/refund")
    @Operation(summary = "Refund a payment")
    @PreAuthorize("hasRole('ADMIN') or @paymentSecurityService.isPaymentOwner(#userId, #id)")
    public Mono<Payment> refundPayment(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String id) {
        return paymentService.refundPayment(id);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a payment by ID")
    @PreAuthorize("hasRole('ADMIN') or @paymentSecurityService.isPaymentOwner(#userId, #id)")
    public Mono<Payment> getPayment(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String id) {
        return paymentService.getPayment(id);
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get all payments for a user")
    @PreAuthorize("hasRole('ADMIN') or #pathUserId == #currentUserId")
    public Flux<Payment> getUserPayments(
            @RequestHeader("X-User-Id") String currentUserId,
            @PathVariable String pathUserId) {
        return paymentService.getUserPayments(pathUserId);
    }

    @GetMapping("/subscription/{subscriptionId}")
    @Operation(summary = "Get payments by subscription ID")
    @PreAuthorize("hasRole('ADMIN') or @subscriptionSecurityService.isSubscriptionOwner(#userId, #subscriptionId)")
    public Flux<Payment> getPaymentsBySubscriptionId(
            @RequestHeader("X-User-Id") String userId,
            @Parameter(description = "Subscription ID", required = true) 
            @PathVariable String subscriptionId) {
        return paymentService.getPaymentsBySubscriptionId(subscriptionId);
    }

    @GetMapping("/pending")
    @Operation(summary = "Get pending payments")
    @PreAuthorize("hasRole('ADMIN')")
    public Flux<Payment> getPendingPayments() {
        return paymentService.getPendingPayments();
    }
} 