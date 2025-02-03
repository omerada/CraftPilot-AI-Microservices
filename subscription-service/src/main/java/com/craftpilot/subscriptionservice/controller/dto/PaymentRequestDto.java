package com.craftpilot.subscriptionservice.controller.dto;
 
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull; 
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequestDto {
    @NotBlank(message = "Subscription ID is required")
    private String subscriptionId;
    
    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;
    
    @NotBlank(message = "Currency is required")
    private String currency;
    
    @NotBlank(message = "Payment method is required")
    private String paymentMethod;
    
    @NotBlank(message = "Card holder name is required")
    private String cardHolderName;
    
    @NotBlank(message = "Card number is required")
    private String cardNumber;
    
    @NotBlank(message = "Expire month is required")
    private String expireMonth;
    
    @NotBlank(message = "Expire year is required")
    private String expireYear;
    
    @NotBlank(message = "CVC is required")
    private String cvc;
} 