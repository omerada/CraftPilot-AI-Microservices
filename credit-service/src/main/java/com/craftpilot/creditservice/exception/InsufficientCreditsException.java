package com.craftpilot.creditservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class InsufficientCreditsException extends ResponseStatusException {
    public InsufficientCreditsException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
} 