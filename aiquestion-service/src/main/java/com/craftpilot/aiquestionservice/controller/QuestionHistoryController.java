package com.craftpilot.aiquestionservice.controller;

import com.craftpilot.aiquestionservice.model.QuestionHistory;
import com.craftpilot.aiquestionservice.service.QuestionHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/question-histories")
@RequiredArgsConstructor
@Tag(name = "Question Histories", description = "Question history management endpoints")
public class QuestionHistoryController {
    private final QuestionHistoryService questionHistoryService;

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get user's question history", description = "Retrieve question history for a specific user")
    public Flux<QuestionHistory> getUserQuestionHistories(
            @Parameter(description = "User ID") @PathVariable String userId) {
        return questionHistoryService.getUserQuestionHistories(userId);
    }

    @GetMapping
    @Operation(summary = "Get all question histories", description = "Retrieve all question histories")
    public Flux<QuestionHistory> getAllQuestionHistories() {
        return questionHistoryService.getAllQuestionHistories();
    }

    @DeleteMapping("/user/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete user's question history", description = "Delete all question histories for a specific user")
    public Mono<Void> deleteQuestionHistory(
            @Parameter(description = "User ID") @PathVariable String userId) {
        return questionHistoryService.deleteQuestionHistory(userId);
    }
} 