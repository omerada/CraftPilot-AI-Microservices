package com.craftpilot.creditservice.controller.dto;

import jakarta.validation.constraints.NotEmpty;
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
public class CreditTransactionRequest {
    @NotEmpty(message = "Service ID boş olamaz")
    private String serviceId;
    
    @NotNull(message = "Miktar boş olamaz")
    @Positive(message = "Miktar pozitif olmalı")
    private BigDecimal amount;
    
    @NotEmpty(message = "İşlem tipi boş olamaz")
    private String type;
    
    private String description;
    
    @NotEmpty(message = "Kredi tipi boş olamaz")
    @Builder.Default
    private String creditType = "STANDARD"; // Varsayılan değer
}