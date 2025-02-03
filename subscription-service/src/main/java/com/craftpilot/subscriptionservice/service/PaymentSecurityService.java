package com.craftpilot.subscriptionservice.service;

import com.craftpilot.subscriptionservice.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service; 

@Service
@RequiredArgsConstructor
public class PaymentSecurityService {
    private final PaymentRepository paymentRepository;

    public boolean isPaymentOwner(String userId, String paymentId) {
        return paymentRepository.findById(paymentId)
                .map(payment -> payment.getUserId().equals(userId))
                .defaultIfEmpty(false)
                .block();
    }
} 