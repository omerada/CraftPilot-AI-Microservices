package com.craftpilot.subscriptionservice.dto;

import com.craftpilot.subscriptionservice.model.payment.enums.PaymentMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;

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