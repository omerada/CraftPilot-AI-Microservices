package com.craftpilot.creditservice.controller;

import com.craftpilot.creditservice.controller.dto.CreditTransactionRequest;
import com.craftpilot.creditservice.controller.dto.CreditDto;
import com.craftpilot.creditservice.controller.dto.CreditTransactionDto;
import com.craftpilot.creditservice.service.CreditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/credits")
@RequiredArgsConstructor
@Tag(name = "Credits", description = "Credit management endpoints")
public class CreditController {
    private final CreditService creditService;

    @GetMapping("/user")
    @Operation(summary = "Get user credits", description = "Retrieve credit balance and details for the current user")
    public Mono<CreditDto> getUserCredits(@RequestHeader("X-User-Id") String userId) {
        return creditService.getUserCredits(userId)
                .map(CreditDto::fromEntity);
    }

    @PostMapping("/transactions")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Process credit transaction", description = "Process a credit transaction (credit or debit)")
    public Mono<CreditTransactionDto> processTransaction(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody CreditTransactionRequest request) {
        return creditService.processTransaction(
                userId,
                request.getServiceId(),
                request.getAmount(),
                request.getType(),
                request.getDescription()
        ).map(CreditTransactionDto::fromEntity);
    }

    @GetMapping("/transactions")
    @Operation(summary = "Get user transactions", description = "Retrieve credit transaction history for the current user")
    public Flux<CreditTransactionDto> getUserTransactions(@RequestHeader("X-User-Id") String userId) {
        return creditService.getUserTransactions(userId)
                .map(CreditTransactionDto::fromEntity);
    }
}