package com.craftpilot.adminservice.exception;

import com.mongodb.MongoTimeoutException;
import com.mongodb.MongoSocketException;
import com.mongodb.MongoExecutionTimeoutException;
import com.mongodb.MongoQueryException;
import com.mongodb.MongoWriteException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler({
            MongoTimeoutException.class,
            MongoSocketException.class,
            MongoExecutionTimeoutException.class,
            DataAccessResourceFailureException.class
    })
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public Mono<ResponseEntity<ErrorResponse>> handleMongoConnectionException(Exception ex) {
        log.error("MongoDB connection error: {}", ex.getMessage(), ex);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.SERVICE_UNAVAILABLE.value())
                .error("MongoDB Connection Error")
                .message("Database connection failed or timed out")
                .timestamp(LocalDateTime.now())
                .path(null) // Path could be added via request context if needed
                .build();

        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse));
    }

    @ExceptionHandler({ MongoQueryException.class, MongoWriteException.class })
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Mono<ResponseEntity<ErrorResponse>> handleMongoOperationException(Exception ex) {
        log.error("MongoDB operation error: {}", ex.getMessage(), ex);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Database Operation Error")
                .message("Failed to perform database operation")
                .timestamp(LocalDateTime.now())
                .path(null)
                .build();

        return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse));
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Mono<ResponseEntity<ErrorResponse>> handleOptimisticLockingFailureException(
            OptimisticLockingFailureException ex) {
        log.error("Optimistic locking failure: {}", ex.getMessage(), ex);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.CONFLICT.value())
                .error("Concurrent Modification")
                .message("The resource was modified by another request")
                .timestamp(LocalDateTime.now())
                .path(null)
                .build();

        return Mono.just(ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse));
    }

    // Add other exception handlers as needed
}
