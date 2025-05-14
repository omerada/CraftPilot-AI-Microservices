package com.craftpilot.notificationservice.exception;

import com.google.firebase.messaging.FirebaseMessagingException;
import com.mongodb.MongoException;
import com.mongodb.MongoTimeoutException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.mongodb.UncategorizedMongoDbException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BaseException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleBaseException(BaseException ex) {
        log.error("Base exception occurred: {}", ex.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(
                ex.getStatus().value(),
                ex.getErrorCode(),
                ex.getMessage(),
                LocalDateTime.now());
        return Mono.just(ResponseEntity.status(ex.getStatus()).body(errorResponse));
    }

    @ExceptionHandler(WebExchangeBindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Mono<ResponseEntity<ErrorResponse>> handleValidationErrors(WebExchangeBindException ex) {
        String errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "VALIDATION_ERROR",
                errors,
                LocalDateTime.now());

        return Mono.just(ResponseEntity.badRequest().body(errorResponse));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Mono<ResponseEntity<ErrorResponse>> handleConstraintViolation(ConstraintViolationException ex) {
        String errors = ex.getConstraintViolations()
                .stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .collect(Collectors.joining(", "));

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "VALIDATION_ERROR",
                errors,
                LocalDateTime.now());

        return Mono.just(ResponseEntity.badRequest().body(errorResponse));
    }

    @ExceptionHandler(CallNotPermittedException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public Mono<ResponseEntity<ErrorResponse>> handleCircuitBreakerException(CallNotPermittedException ex) {
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.SERVICE_UNAVAILABLE.value(),
                "CIRCUIT_BREAKER_ERROR",
                "Servis geçici olarak kullanılamıyor. Lütfen daha sonra tekrar deneyin.",
                LocalDateTime.now());

        log.error("Circuit breaker opened: {}", ex.getMessage());
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse));
    }

    @ExceptionHandler(FirebaseMessagingException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public Mono<ResponseEntity<ErrorResponse>> handleFirebaseMessagingException(FirebaseMessagingException ex) {
        log.error("Firebase messaging error: {}", ex.getMessage());

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.SERVICE_UNAVAILABLE.value(),
                "FIREBASE_ERROR",
                "Bildirim gönderilirken bir hata oluştu",
                LocalDateTime.now());

        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse));
    }

    @ExceptionHandler({ MongoException.class, UncategorizedMongoDbException.class,
            DataAccessResourceFailureException.class })
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public Mono<ResponseEntity<ErrorResponse>> handleMongoDbException(Exception ex) {
        log.error("MongoDB error: {}", ex.getMessage());

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.SERVICE_UNAVAILABLE.value(),
                "DATABASE_ERROR",
                "Veritabanı işlemi başarısız oldu",
                LocalDateTime.now());

        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse));
    }

    @ExceptionHandler(MongoTimeoutException.class)
    @ResponseStatus(HttpStatus.GATEWAY_TIMEOUT)
    public Mono<ResponseEntity<ErrorResponse>> handleMongoTimeoutException(MongoTimeoutException ex) {
        log.error("MongoDB timeout: {}", ex.getMessage());

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.GATEWAY_TIMEOUT.value(),
                "DATABASE_TIMEOUT",
                "Veritabanı işlemi zaman aşımına uğradı",
                LocalDateTime.now());

        return Mono.just(ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).body(errorResponse));
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Mono<ResponseEntity<ErrorResponse>> handleAllUncaughtException(Exception ex) {
        log.error("Unexpected error occurred: ", ex);

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "INTERNAL_SERVER_ERROR",
                "Beklenmeyen bir hata oluştu",
                LocalDateTime.now());

        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse));
    }
}

record ErrorResponse(
        int status,
        String error,
        String message,
        LocalDateTime timestamp) {
}