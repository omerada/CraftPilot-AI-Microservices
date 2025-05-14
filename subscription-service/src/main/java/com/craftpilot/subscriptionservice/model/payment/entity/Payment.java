package com.craftpilot.subscriptionservice.model.payment.entity;

import com.craftpilot.subscriptionservice.model.payment.enums.PaymentMethod;
import com.craftpilot.subscriptionservice.model.payment.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "payments")
public class Payment {
    @Id
    private String id;

    @Indexed
    private String userId;

    @Indexed
    private String subscriptionId;

    private BigDecimal amount;
    private String currency;
    private String description;
    private PaymentStatus status;
    private PaymentMethod paymentMethod;
    private String cardHolderName;
    private String cardNumber;
    private String expireMonth;
    private String expireYear;
    private String cvc;
    private String transactionId;
    private String iyzicoPaymentTransactionId;
    private String refundTransactionId;
    private String errorMessage;
    private LocalDateTime paymentDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}