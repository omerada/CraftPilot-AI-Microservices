package com.craftpilot.notificationservice.service;

import com.craftpilot.notificationservice.dto.NotificationRequest;
import com.craftpilot.notificationservice.dto.NotificationResponse;
import com.craftpilot.notificationservice.model.Notification;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface NotificationService {
    
    Mono<NotificationResponse> createNotification(NotificationRequest request);
    
    Mono<NotificationResponse> getNotification(String id);
    
    Flux<NotificationResponse> getUserNotifications(String userId, boolean onlyUnread);
    
    Mono<NotificationResponse> markAsRead(String id);
    
    Mono<Void> deleteNotification(String id);
    
    Flux<Notification> getScheduledNotifications();
    
    Mono<Void> markAsSent(String id);
} 