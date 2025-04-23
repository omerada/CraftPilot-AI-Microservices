package com.craftpilot.creditservice.event;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class CreditEvent {
    private String userId;
    private BigDecimal amount;
    private String type;
    private String creditType;
    private long timestamp;
}