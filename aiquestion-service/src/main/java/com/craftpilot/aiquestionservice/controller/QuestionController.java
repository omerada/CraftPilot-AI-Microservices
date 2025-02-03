package com.craftpilot.aiquestionservice.controller;

import com.craftpilot.aiquestionservice.controller.dto.QuestionRequest;
import com.craftpilot.aiquestionservice.controller.dto.QuestionResponse;
import com.craftpilot.aiquestionservice.model.Question;
import com.craftpilot.aiquestionservice.model.enums.QuestionStatus;
import com.craftpilot.aiquestionservice.model.enums.QuestionType;
import com.craftpilot.aiquestionservice.service.QuestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/v1/questions")
@RequiredArgsConstructor
@Tag(name = "Questions", description = "Question management endpoints")
public class QuestionController {
    private final QuestionService questionService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new question", description = "Create a new question with the provided details")
    public Mono<QuestionResponse> createQuestion(@RequestBody QuestionRequest request) {
        Question question = Question.builder()
                .content(request.getContent())
                .context(request.getContext())
                .question(request.getQuestion())
                .type(request.getType())
                .modelId(request.getModelId())
                .parameters(request.getParameters())
                .preferences(request.getPreferences())
                .tags(request.getTags())
                .useWebSearch(request.getUseWebSearch())
                .build();

        return questionService.createQuestion(question)
                .map(QuestionResponse::fromModel);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a question", description = "Update an existing question with new details")
    public Mono<QuestionResponse> updateQuestion(
            @Parameter(description = "Question ID") @PathVariable String id,
            @RequestBody QuestionRequest request) {
        Question question = Question.builder()
                .content(request.getContent())
                .context(request.getContext())
                .question(request.getQuestion())
                .type(request.getType())
                .modelId(request.getModelId())
                .parameters(request.getParameters())
                .preferences(request.getPreferences())
                .tags(request.getTags())
                .useWebSearch(request.getUseWebSearch())
                .build();

        return questionService.updateQuestion(id, question)
                .map(QuestionResponse::fromModel);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a question", description = "Retrieve a question by its ID")
    public Mono<QuestionResponse> getQuestion(
            @Parameter(description = "Question ID") @PathVariable String id) {
        return questionService.getQuestionById(id)
                .map(QuestionResponse::fromModel);
    }

    @GetMapping
    @Operation(summary = "Get all questions", description = "Retrieve all questions")
    public Flux<QuestionResponse> getAllQuestions() {
        return questionService.getAllQuestions()
                .map(QuestionResponse::fromModel);
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get user's questions", description = "Retrieve all questions for a specific user")
    public Flux<QuestionResponse> getQuestionsByUser(
            @Parameter(description = "User ID") @PathVariable String userId) {
        return questionService.getQuestionsByUser(userId)
                .map(QuestionResponse::fromModel);
    }

    @GetMapping("/type/{type}")
    @Operation(summary = "Get questions by type", description = "Retrieve all questions of a specific type")
    public Flux<QuestionResponse> getQuestionsByType(
            @Parameter(description = "Question type") @PathVariable QuestionType type) {
        return questionService.getQuestionsByType(type)
                .map(QuestionResponse::fromModel);
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get questions by status", description = "Retrieve all questions with a specific status")
    public Flux<QuestionResponse> getQuestionsByStatus(
            @Parameter(description = "Question status") @PathVariable QuestionStatus status) {
        return questionService.getQuestionsByStatus(status)
                .map(QuestionResponse::fromModel);
    }

    @GetMapping("/tags")
    @Operation(summary = "Get questions by tags", description = "Retrieve all questions with specific tags")
    public Flux<QuestionResponse> getQuestionsByTags(
            @Parameter(description = "List of tags") @RequestParam List<String> tags) {
        return questionService.getQuestionsByTags(tags)
                .map(QuestionResponse::fromModel);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a question", description = "Delete a question by its ID")
    public Mono<Void> deleteQuestion(
            @Parameter(description = "Question ID") @PathVariable String id) {
        return questionService.deleteQuestion(id);
    }
} 