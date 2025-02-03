package com.craftpilot.contentservice.controller;

import com.craftpilot.contentservice.model.Credit;
import com.craftpilot.contentservice.service.CreditService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/credits")
@RequiredArgsConstructor
public class CreditController {

    private final CreditService creditService;

    @PostMapping("/{userId}")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Credit> createCredit(@PathVariable String userId) {
        return creditService.createCredit(userId);
    }

    @GetMapping("/{userId}")
    public Mono<Credit> getCredit(@PathVariable String userId) {
        return creditService.getCredit(userId);
    }

    @PutMapping("/{userId}")
    public Mono<Credit> updateCredits(@PathVariable String userId, @RequestParam int credits) {
        return creditService.updateCredits(userId, credits);
    }

    @PutMapping("/{userId}/deduct")
    public Mono<Credit> deductCredits(@PathVariable String userId, @RequestParam int credits) {
        return creditService.deductCredits(userId, credits);
    }

    @PutMapping("/{userId}/add")
    public Mono<Credit> addCredits(@PathVariable String userId, @RequestParam int credits) {
        return creditService.addCredits(userId, credits);
    }

    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteCredit(@PathVariable String userId) {
        return creditService.deleteCredit(userId);
    }
} 