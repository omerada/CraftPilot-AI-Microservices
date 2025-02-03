package com.craftpilot.notificationservice.service;

import com.craftpilot.notificationservice.model.Notification;
import reactor.core.publisher.Mono;

public interface PushNotificationService {
    Mono<Void> sendPushNotification(Notification notification);
} 