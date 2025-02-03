package com.craftpilot.subscriptionservice.model.payment.entity;

import com.craftpilot.subscriptionservice.model.payment.enums.PaymentMethod;
import com.craftpilot.subscriptionservice.model.payment.enums.PaymentStatus;
import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.firestore.annotation.PropertyName;
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
public class Payment {
    @DocumentId
    private String id;
    
    @PropertyName("userId")
    private String userId;
    
    @PropertyName("subscriptionId")
    private String subscriptionId;
    
    @PropertyName("amount")
    private BigDecimal amount;
    
    @PropertyName("currency")
    private String currency;
    
    @PropertyName("status")
    private PaymentStatus status;
    
    @PropertyName("paymentMethod")
    private PaymentMethod paymentMethod;
    
    @PropertyName("cardHolderName")
    private String cardHolderName;
    
    @PropertyName("cardNumber")
    private String cardNumber;
    
    @PropertyName("expireMonth")
    private String expireMonth;
    
    @PropertyName("expireYear")
    private String expireYear;
    
    @PropertyName("cvc")
    private String cvc;
    
    @PropertyName("iyzicoPaymentTransactionId")
    private String iyzicoPaymentTransactionId;
    
    @PropertyName("transactionId")
    private String transactionId;
    
    @PropertyName("refundTransactionId")
    private String refundTransactionId;
    
    @PropertyName("errorMessage")
    private String errorMessage;
    
    @PropertyName("createdAt")
    private LocalDateTime createdAt;
    
    @PropertyName("updatedAt")
    private LocalDateTime updatedAt;
}