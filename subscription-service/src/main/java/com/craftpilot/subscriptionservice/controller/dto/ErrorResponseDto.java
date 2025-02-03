package com.craftpilot.subscriptionservice.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponseDto {
    private String error;
    private String message;
    private int status;
    private long timestamp;

    public static ErrorResponseDto of(String error, String message, int status) {
        return ErrorResponseDto.builder()
                .error(error)
                .message(message)
                .status(status)
                .timestamp(System.currentTimeMillis())
                .build();
    }
} 