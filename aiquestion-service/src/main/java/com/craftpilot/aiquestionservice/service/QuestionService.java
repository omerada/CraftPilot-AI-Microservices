package com.craftpilot.aiquestionservice.service;

import com.craftpilot.aiquestionservice.exception.QuestionNotFoundException;
import com.craftpilot.aiquestionservice.model.Question;
import com.craftpilot.aiquestionservice.model.enums.QuestionStatus;
import com.craftpilot.aiquestionservice.model.enums.QuestionType;
import com.craftpilot.aiquestionservice.repository.QuestionRepository;
import com.google.cloud.Timestamp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionService {
    private final QuestionRepository questionRepository;

    public Mono<Question> createQuestion(Question question) {
        if (question.getId() == null) {
            question.setId(java.util.UUID.randomUUID().toString());
        }

        if (question.getCreatedAt() == null) {
            question.setCreatedAt(Timestamp.now());
        }
        question.setUpdatedAt(Timestamp.now());

        return questionRepository.save(question)
                .doOnSuccess(savedQuestion -> log.info("Question created successfully with ID: {}", savedQuestion.getId()))
                .doOnError(error -> log.error("Error creating Question", error));
    }

    public Mono<Question> getQuestionById(String id) {
        return questionRepository.findById(id)
                .doOnSuccess(question -> {
                    if (question != null) {
                        log.info("Question found with ID: {}", id);
                    } else {
                        log.info("No Question found with ID: {}", id);
                    }
                })
                .doOnError(error -> log.error("Error retrieving Question with ID: {}", id, error))
                .switchIfEmpty(Mono.error(new QuestionNotFoundException("Question not found with ID: " + id)));
    }

    public Mono<Question> updateQuestion(String id, Question question) {
        question.setId(id);
        return questionRepository.findById(id)
                .flatMap(existingQuestion -> {
                    question.setCreatedAt(existingQuestion.getCreatedAt());
                    question.setUpdatedAt(Timestamp.now());
                    return questionRepository.save(question);
                })
                .doOnSuccess(updatedQuestion -> log.info("Question updated successfully with ID: {}", id))
                .doOnError(error -> log.error("Error updating Question with ID: {}", id, error))
                .switchIfEmpty(Mono.error(new QuestionNotFoundException("Question not found with ID: " + id)));
    }

    public Mono<Void> deleteQuestion(String id) {
        return questionRepository.findById(id)
                .flatMap(question -> questionRepository.deleteById(id))
                .doOnSuccess(result -> log.info("Question deleted successfully with ID: {}", id))
                .doOnError(error -> log.error("Error deleting Question with ID: {}", id, error))
                .switchIfEmpty(Mono.error(new QuestionNotFoundException("Question not found with ID: " + id)));
    }

    public Flux<Question> getAllQuestions() {
        return questionRepository.findAll()
                .doOnComplete(() -> log.info("Retrieved all Questions"))
                .doOnError(error -> log.error("Error retrieving all Questions", error));
    }

    public Flux<Question> getQuestionsByUser(String userId) {
        return questionRepository.findByUserId(userId)
                .doOnComplete(() -> log.info("Retrieved all Questions for user ID: {}", userId))
                .doOnError(error -> log.error("Error retrieving Questions for user ID: {}", userId, error));
    }

    public Flux<Question> getQuestionsByType(QuestionType type) {
        return questionRepository.findByType(type)
                .doOnComplete(() -> log.info("Retrieved all Questions of type: {}", type))
                .doOnError(error -> log.error("Error retrieving Questions of type: {}", type, error));
    }

    public Flux<Question> getQuestionsByStatus(QuestionStatus status) {
        return questionRepository.findByStatus(status)
                .doOnComplete(() -> log.info("Retrieved all Questions with status: {}", status))
                .doOnError(error -> log.error("Error retrieving Questions with status: {}", status, error));
    }

    public Flux<Question> getQuestionsByTags(List<String> tags) {
        return Flux.fromIterable(tags)
                .flatMap(tag -> questionRepository.findByTagsContaining(tag))
                .distinct()
                .doOnComplete(() -> log.info("Retrieved all Questions with tags: {}", tags))
                .doOnError(error -> log.error("Error retrieving Questions with tags: {}", tags, error));
    }
} 