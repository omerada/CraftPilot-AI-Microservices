package com.craftpilot.subscriptionservice.service;

import com.craftpilot.subscriptionservice.model.payment.PaymentRequest;
import com.craftpilot.subscriptionservice.model.payment.iyzico.IyzicoPaymentResult;
import com.craftpilot.subscriptionservice.model.payment.iyzico.IyzicoRefundResult;
import com.craftpilot.subscriptionservice.model.payment.iyzico.request.CreatePaymentRequest;
import com.craftpilot.subscriptionservice.model.payment.iyzico.request.CreateRefundRequest;
import reactor.core.publisher.Mono;

public interface IyzicoService {
    Mono<String> createPaymentLink(PaymentRequest request);
    Mono<IyzicoPaymentResult> createPayment(CreatePaymentRequest request);
    Mono<IyzicoRefundResult> createRefund(CreateRefundRequest request);
    Mono<Void> handlePaymentCallback(String token);
} 