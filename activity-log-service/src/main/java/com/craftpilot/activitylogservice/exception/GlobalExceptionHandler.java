package com.craftpilot.activitylogservice.exception;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Mono<Map<String, Object>> handleValidationException(ValidationException ex) {
        log.warn("Validation error: {}", ex.getMessage());
        
        Map<String, Object> errorMap = new HashMap<>();
        errorMap.put("status", HttpStatus.BAD_REQUEST.value());
        errorMap.put("error", "Validation Error");
        errorMap.put("message", ex.getMessage());
        errorMap.put("timestamp", LocalDateTime.now());
        
        return Mono.just(errorMap);
    }

    @ExceptionHandler(WebExchangeBindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Mono<Map<String, Object>> handleValidationErrors(WebExchangeBindException ex) {
        log.warn("Binding error: {}", ex.getMessage());
        
        Map<String, Object> errorMap = new HashMap<>();
        errorMap.put("status", HttpStatus.BAD_REQUEST.value());
        errorMap.put("error", "Validation Error");
        
        String errorMessage = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .collect(Collectors.joining(", "));
        
        errorMap.put("message", errorMessage);
        errorMap.put("timestamp", LocalDateTime.now());
        
        return Mono.just(errorMap);
    }

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<Map<String, Object>>> handleGenericException(Exception ex) {
        log.error("Unhandled exception", ex);
        
        Map<String, Object> errorMap = new HashMap<>();
        errorMap.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        errorMap.put("error", "Internal Server Error");
        errorMap.put("message", "An unexpected error occurred");
        errorMap.put("timestamp", LocalDateTime.now());
        
        return Mono.just(ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorMap));
    }

    @ExceptionHandler(DataAccessException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public Mono<ResponseEntity<ErrorResponse>> handleMongoDBException(DataAccessException ex) {
        log.error("MongoDB error: {}", ex.getMessage());
        
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.SERVICE_UNAVAILABLE.value(),
                "DATABASE_ERROR",
                "Veritabanı işlemi başarısız oldu",
                LocalDateTime.now()
        );

        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse));
    }
    
    @ExceptionHandler(CallNotPermittedException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public Mono<ResponseEntity<ErrorResponse>> handleCircuitBreakerException(CallNotPermittedException ex) {
        log.error("Circuit breaker open: {}", ex.getMessage());
        
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.SERVICE_UNAVAILABLE.value(),
                "SERVICE_UNAVAILABLE",
                "Servis geçici olarak kullanılamıyor, lütfen daha sonra tekrar deneyin",
                LocalDateTime.now()
        );

        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse));
    }
}
