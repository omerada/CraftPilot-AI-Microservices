package com.craftpilot.subscriptionservice.controller.dto;

import com.craftpilot.subscriptionservice.model.payment.entity.Payment;
import com.craftpilot.subscriptionservice.model.payment.enums.PaymentMethod;
import com.craftpilot.subscriptionservice.model.payment.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentDto {
    private String id;
    private String userId;
    private String subscriptionId;
    private BigDecimal amount;
    private String currency;
    private String paymentMethod;
    private String cardHolderName;
    private String cardNumber;
    private String expireMonth;
    private String expireYear;
    private String cvc;
    private String status;
    private String iyzicoPaymentTransactionId;
    private String refundTransactionId;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static PaymentDto fromEntity(Payment payment) {
        return PaymentDto.builder()
                .id(payment.getId())
                .userId(payment.getUserId())
                .subscriptionId(payment.getSubscriptionId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .paymentMethod(payment.getPaymentMethod().name())
                .cardHolderName(payment.getCardHolderName())
                .cardNumber(payment.getCardNumber())
                .expireMonth(payment.getExpireMonth())
                .expireYear(payment.getExpireYear())
                .cvc(payment.getCvc())
                .status(payment.getStatus().toString())
                .iyzicoPaymentTransactionId(payment.getIyzicoPaymentTransactionId())
                .refundTransactionId(payment.getRefundTransactionId())
                .errorMessage(payment.getErrorMessage())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }

    public Payment toEntity() {
        return Payment.builder()
                .id(this.id)
                .userId(this.userId)
                .subscriptionId(this.subscriptionId)
                .amount(this.amount) // Using BigDecimal directly
                .currency(this.currency)
                .paymentMethod(this.paymentMethod != null ? PaymentMethod.valueOf(this.paymentMethod) : null)
                .cardHolderName(this.cardHolderName)
                .cardNumber(this.cardNumber)
                .expireMonth(this.expireMonth)
                .expireYear(this.expireYear)
                .cvc(this.cvc)
                .status(this.status != null ? PaymentStatus.valueOf(this.status) : null)
                .iyzicoPaymentTransactionId(this.iyzicoPaymentTransactionId)
                .refundTransactionId(this.refundTransactionId)
                .errorMessage(this.errorMessage)
                .createdAt(this.createdAt)
                .updatedAt(this.updatedAt)
                .build();
    }
}