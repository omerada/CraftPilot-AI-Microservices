package com.craftpilot.subscriptionservice.exception.handler;

import com.craftpilot.subscriptionservice.controller.dto.ErrorResponseDto;
import com.craftpilot.subscriptionservice.exception.DuplicatePlanNameException;
import com.craftpilot.subscriptionservice.exception.PaymentNotFoundException;
import com.craftpilot.subscriptionservice.exception.SubscriptionPlanNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import reactor.core.publisher.Mono;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(WebExchangeBindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Mono<ErrorResponseDto> handleValidationException(WebExchangeBindException ex) {
        String errorMessage = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        log.error("Validation error: {}", errorMessage);

        return Mono.just(ErrorResponseDto.of("Validation Error", errorMessage, HttpStatus.BAD_REQUEST.value()));
    }

    @ExceptionHandler(SubscriptionPlanNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Mono<ErrorResponseDto> handleSubscriptionNotFoundException(SubscriptionPlanNotFoundException ex) {
        log.error("Subscription not found: {}", ex.getMessage());
        return Mono.just(ErrorResponseDto.of("Not Found", ex.getMessage(), HttpStatus.NOT_FOUND.value()));
    }

    @ExceptionHandler(PaymentNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Mono<ErrorResponseDto> handlePaymentNotFoundException(PaymentNotFoundException ex) {
        log.error("Payment not found: {}", ex.getMessage());
        return Mono.just(ErrorResponseDto.of("Not Found", ex.getMessage(), HttpStatus.NOT_FOUND.value()));
    }

    @ExceptionHandler(DuplicatePlanNameException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Mono<ErrorResponseDto> handleDuplicatePlanNameException(DuplicatePlanNameException ex) {
        log.error("Duplicate plan name: {}", ex.getMessage());
        return Mono.just(ErrorResponseDto.of("Conflict", ex.getMessage(), HttpStatus.CONFLICT.value()));
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Mono<ErrorResponseDto> handleIllegalStateException(IllegalStateException ex) {
        log.error("Illegal state: {}", ex.getMessage());
        return Mono.just(ErrorResponseDto.of("Bad Request", ex.getMessage(), HttpStatus.BAD_REQUEST.value()));
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Mono<ErrorResponseDto> handleGenericException(Exception ex) {
        log.error("Internal server error", ex);
        return Mono.just(ErrorResponseDto.of(
                "Internal Server Error",
                "An unexpected error occurred",
                HttpStatus.INTERNAL_SERVER_ERROR.value()
        ));
    }
} 