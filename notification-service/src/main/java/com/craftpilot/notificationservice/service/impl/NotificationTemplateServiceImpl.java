package com.craftpilot.notificationservice.service.impl;

import com.craftpilot.notificationservice.dto.NotificationTemplateRequest;
import com.craftpilot.notificationservice.dto.NotificationTemplateResponse;
import com.craftpilot.notificationservice.model.NotificationTemplate;
import com.craftpilot.notificationservice.repository.NotificationTemplateRepository;
import com.craftpilot.notificationservice.service.NotificationTemplateService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationTemplateServiceImpl implements NotificationTemplateService {

    private final NotificationTemplateRepository templateRepository;
    private final MeterRegistry meterRegistry;

    @Override
    @CircuitBreaker(name = "createTemplate")
    @Retry(name = "createTemplate")
    public Mono<NotificationTemplateResponse> createTemplate(NotificationTemplateRequest request) {
        NotificationTemplate template = new NotificationTemplate();
        template.setName(request.getName());
        template.setTitleTemplate(request.getTitleTemplate());
        template.setContentTemplate(request.getContentTemplate());
        template.setRequiredVariables(request.getRequiredVariables());
        template.setDefaultValues(request.getDefaultValues());
        template.setActive(request.isActive());
        template.setDeleted(false);
        template.setCreatedAt(LocalDateTime.now());

        return templateRepository.save(template)
                .map(NotificationTemplateResponse::fromEntity)
                .doOnSuccess(response -> {
                    meterRegistry.counter("template.created").increment();
                    log.info("Template created successfully: {}", response.getId());
                })
                .doOnError(error -> {
                    meterRegistry.counter("template.creation.failed").increment();
                    log.error("Failed to create template", error);
                });
    }

    @Override
    public Mono<NotificationTemplate> getTemplate(String id) {
        return templateRepository.findById(id)
                .doOnSuccess(template -> log.debug("Retrieved template: {}", id));
    }

    @Override
    public Flux<NotificationTemplateResponse> getAllActiveTemplates() {
        return templateRepository.findByActiveAndDeleted(true, false)
                .map(NotificationTemplateResponse::fromEntity)
                .doOnComplete(() -> log.debug("Retrieved all active templates"));
    }

    @Override
    public Mono<NotificationTemplateResponse> updateTemplate(String id, NotificationTemplateRequest request) {
        return templateRepository.findById(id)
                .flatMap(template -> {
                    template.setName(request.getName());
                    template.setTitleTemplate(request.getTitleTemplate());
                    template.setContentTemplate(request.getContentTemplate());
                    template.setRequiredVariables(request.getRequiredVariables());
                    template.setDefaultValues(request.getDefaultValues());
                    template.setActive(request.isActive());
                    template.setUpdatedAt(LocalDateTime.now());
                    return templateRepository.save(template);
                })
                .map(NotificationTemplateResponse::fromEntity)
                .doOnSuccess(response -> {
                    meterRegistry.counter("template.updated").increment();
                    log.info("Template updated successfully: {}", id);
                });
    }

    @Override
    public Mono<Void> deleteTemplate(String id) {
        return templateRepository.findById(id)
                .flatMap(template -> {
                    template.setDeleted(true);
                    template.setActive(false);
                    template.setUpdatedAt(LocalDateTime.now());
                    return templateRepository.save(template);
                })
                .then()
                .doOnSuccess(unused -> {
                    meterRegistry.counter("template.deleted").increment();
                    log.info("Template deleted: {}", id);
                });
    }

    @Override
    public Mono<NotificationTemplateResponse> activateTemplate(String id) {
        return updateTemplateStatus(id, true);
    }

    @Override
    public Mono<NotificationTemplateResponse> deactivateTemplate(String id) {
        return updateTemplateStatus(id, false);
    }

    private Mono<NotificationTemplateResponse> updateTemplateStatus(String id, boolean active) {
        return templateRepository.findById(id)
                .flatMap(template -> {
                    template.setActive(active);
                    template.setUpdatedAt(LocalDateTime.now());
                    return templateRepository.save(template);
                })
                .map(NotificationTemplateResponse::fromEntity)
                .doOnSuccess(response -> {
                    meterRegistry.counter("template.status.updated").increment();
                    log.info("Template status updated to {}: {}", active ? "active" : "inactive", id);
                });
    }
} 