package com.craftpilot.subscriptionservice.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationErrorResponse {
    private LocalDateTime timestamp;
    private HttpStatus status;
    private String errorCode;
    private String message;
    private Map<String, String> errors;
} 