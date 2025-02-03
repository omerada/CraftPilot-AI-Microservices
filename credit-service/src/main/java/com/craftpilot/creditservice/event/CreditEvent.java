package com.craftpilot.creditservice.event;

import com.craftpilot.creditservice.model.CreditTransaction;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreditEvent {
    private CreditTransaction transaction;
} 