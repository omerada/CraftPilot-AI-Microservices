package com.craftpilot.subscriptionservice.service;

import com.craftpilot.subscriptionservice.controller.dto.PaymentRequestDto;
import com.craftpilot.subscriptionservice.event.PaymentEvent;
import com.craftpilot.subscriptionservice.model.payment.PaymentRequest;
import com.craftpilot.subscriptionservice.model.payment.PaymentResponse;
import com.craftpilot.subscriptionservice.model.payment.entity.Payment;
import com.craftpilot.subscriptionservice.model.payment.enums.PaymentMethod;
import com.craftpilot.subscriptionservice.model.payment.enums.PaymentStatus;
import com.craftpilot.subscriptionservice.model.payment.iyzico.IyzicoPaymentResult;
import com.craftpilot.subscriptionservice.model.payment.iyzico.IyzicoRefundResult;
import com.craftpilot.subscriptionservice.model.payment.iyzico.request.CreatePaymentRequest;
import com.craftpilot.subscriptionservice.model.payment.iyzico.request.CreateRefundRequest;
import com.craftpilot.subscriptionservice.repository.FirestorePaymentRepository;
import com.craftpilot.subscriptionservice.model.subscription.entity.Subscription;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final IyzicoService iyzicoService;
    private final FirestorePaymentRepository paymentRepository;
    private final KafkaTemplate<String, PaymentEvent> kafkaTemplate;

    @CircuitBreaker(name = "payment-service")
    public Mono<Payment> processPayment(String userId, PaymentRequestDto paymentRequest) {
        log.info("Processing payment for user: {}", userId);

        Payment payment = Payment.builder()
                .userId(userId)
                .subscriptionId(paymentRequest.getSubscriptionId())
                .amount(paymentRequest.getAmount())
                .currency(paymentRequest.getCurrency())
                .paymentMethod(PaymentMethod.valueOf(paymentRequest.getPaymentMethod()))
                .cardHolderName(paymentRequest.getCardHolderName())
                .cardNumber(paymentRequest.getCardNumber())
                .expireMonth(paymentRequest.getExpireMonth())
                .expireYear(paymentRequest.getExpireYear())
                .cvc(paymentRequest.getCvc())
                .status(PaymentStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return paymentRepository.save(payment)
                .flatMap(savedPayment -> {
                    CreatePaymentRequest createPaymentRequest = CreatePaymentRequest.builder()
                            .userId(userId)
                            .subscriptionId(savedPayment.getSubscriptionId())
                            .amount(savedPayment.getAmount())
                            .currency(savedPayment.getCurrency())
                            .cardHolderName(savedPayment.getCardHolderName())
                            .cardNumber(savedPayment.getCardNumber())
                            .expireMonth(savedPayment.getExpireMonth())
                            .expireYear(savedPayment.getExpireYear())
                            .cvc(savedPayment.getCvc())
                            .build();

                    return iyzicoService.createPayment(createPaymentRequest)
                            .flatMap(result -> handlePaymentResult(savedPayment, result));
                });
    }

    @CircuitBreaker(name = "payment-service")
    public Mono<Payment> refundPayment(String paymentId) {
        log.info("Refunding payment: {}", paymentId);

        return paymentRepository.findById(paymentId)
                .switchIfEmpty(Mono.error(new RuntimeException("Payment not found")))
                .flatMap(payment -> {
                    CreateRefundRequest refundRequest = CreateRefundRequest.builder()
                            .paymentTransactionId(payment.getIyzicoPaymentTransactionId())
                            .amount(payment.getAmount())
                            .currency(payment.getCurrency())
                            .reason("Customer requested refund")
                            .build();

                    return iyzicoService.createRefund(refundRequest)
                            .flatMap(result -> handleRefundResult(payment, result));
                });
    }

    @CircuitBreaker(name = "payment-service")
    public Mono<Payment> getPayment(String paymentId) {
        log.info("Getting payment details for ID: {}", paymentId);
        return paymentRepository.findById(paymentId);
    }

    @CircuitBreaker(name = "payment-service")
    public Flux<Payment> getUserPayments(String userId) {
        log.info("Getting all payments for user: {}", userId);
        return paymentRepository.findByUserId(userId);
    }

    @CircuitBreaker(name = "payment-service")
    public Flux<Payment> getPaymentsBySubscriptionId(String subscriptionId) {
        log.info("Getting payments for subscription: {}", subscriptionId);
        return paymentRepository.findBySubscriptionId(subscriptionId);
    }

    @CircuitBreaker(name = "payment-service")
    public Flux<Payment> getPendingPayments() {
        log.info("Getting all pending payments");
        return paymentRepository.findPendingPayments();
    }

    private Mono<Payment> handlePaymentResult(Payment payment, IyzicoPaymentResult result) {
        log.info("Handling payment result for payment: {}", payment.getId());

        if ("success".equalsIgnoreCase(result.getStatus())) {
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setIyzicoPaymentTransactionId(result.getPaymentTransactionId());
            payment.setUpdatedAt(LocalDateTime.now());
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setErrorMessage(result.getErrorMessage());
            payment.setUpdatedAt(LocalDateTime.now());
        }

        return paymentRepository.save(payment)
                .doOnSuccess(savedPayment -> sendPaymentEvent(savedPayment));
    }

    private Mono<Payment> handleRefundResult(Payment payment, IyzicoRefundResult result) {
        log.info("Handling refund result for payment: {}", payment.getId());

        if ("success".equalsIgnoreCase(result.getStatus())) {
            payment.setStatus(PaymentStatus.REFUNDED);
            payment.setUpdatedAt(LocalDateTime.now());
        } else {
            payment.setStatus(PaymentStatus.REFUND_FAILED);
            payment.setErrorMessage(result.getErrorMessage());
            payment.setUpdatedAt(LocalDateTime.now());
        }

        return paymentRepository.save(payment)
                .doOnSuccess(savedPayment -> sendPaymentEvent(savedPayment));
    }

    private void sendPaymentEvent(Payment payment) {
        log.info("Sending payment event for payment: {}", payment.getId());

        PaymentEvent event = PaymentEvent.builder()
                .eventType("PAYMENT_" + payment.getStatus().name())
                .paymentId(payment.getId())
                .userId(payment.getUserId())
                .subscriptionId(payment.getSubscriptionId())
                .amount(payment.getAmount())
                .status(payment.getStatus().toString())
                .timestamp(System.currentTimeMillis())
                .build();

        kafkaTemplate.send("payment-events", event.getPaymentId(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to send payment event: {}", event, ex);
                    } else {
                        log.debug("Sent payment event: {}", event);
                    }
                });
    }

    public Mono<PaymentResponse> createPayment(Subscription subscription) {
        log.info("Creating payment for subscription: {}", subscription.getId());

        PaymentRequest paymentRequest = PaymentRequest.builder()
                .userId(subscription.getUserId())
                .planId(subscription.getPlanId())
                .amount(subscription.getAmount())
                .description(subscription.getDescription())
                .callbackUrl("http://localhost:8080/api/v1/payments/callback")
                .build();

        return iyzicoService.createPaymentLink(paymentRequest)
                .map(paymentUrl -> {
                    sendPaymentEvent(subscription, "PAYMENT_CREATED", paymentUrl);
                    return PaymentResponse.builder()
                            .paymentUrl(paymentUrl)
                            .build();
                });
    }

    public Mono<Void> handlePaymentCallback(String token) {
        return iyzicoService.handlePaymentCallback(token);
    }

    private void sendPaymentEvent(Subscription subscription, String eventType, String paymentUrl) {
        PaymentEvent event = PaymentEvent.builder()
                .eventType(eventType)
                .subscriptionId(subscription.getId())
                .userId(subscription.getUserId())
                .amount(subscription.getAmount())
                .status("PENDING")
                .timestamp(LocalDateTime.now().toEpochSecond(java.time.ZoneOffset.UTC))
                .build();

        kafkaTemplate.send("payment-events", subscription.getId(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to send payment event: {}", event, ex);
                    } else {
                        log.debug("Sent payment event: {}", event);
                    }
                });
    }
} 