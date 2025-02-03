package com.craftpilot.notificationservice.service;

import com.craftpilot.notificationservice.dto.NotificationTemplateRequest;
import com.craftpilot.notificationservice.dto.NotificationTemplateResponse;
import com.craftpilot.notificationservice.model.NotificationTemplate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface NotificationTemplateService {
    
    Mono<NotificationTemplateResponse> createTemplate(NotificationTemplateRequest request);
    
    Mono<NotificationTemplate> getTemplate(String id);
    
    Flux<NotificationTemplateResponse> getAllActiveTemplates();
    
    Mono<NotificationTemplateResponse> updateTemplate(String id, NotificationTemplateRequest request);
    
    Mono<Void> deleteTemplate(String id);
    
    Mono<NotificationTemplateResponse> activateTemplate(String id);
    
    Mono<NotificationTemplateResponse> deactivateTemplate(String id);
} 