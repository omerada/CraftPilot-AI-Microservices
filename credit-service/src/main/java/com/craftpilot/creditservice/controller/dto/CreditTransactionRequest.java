package com.craftpilot.creditservice.controller.dto;

import com.craftpilot.creditservice.model.CreditTransaction;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreditTransactionRequest {
    @NotBlank(message = "Service ID is required")
    private String serviceId;

    @NotNull(message = "Transaction type is required")
    private CreditTransaction.TransactionType type;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;

    @NotBlank(message = "Description is required")
    private String description;
} 