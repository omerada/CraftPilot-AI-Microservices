package com.craftpilot.notificationservice.service.impl;

import com.craftpilot.notificationservice.exception.InvalidNotificationParametersException;
import com.craftpilot.notificationservice.exception.NotificationDeliveryException;
import com.craftpilot.notificationservice.model.Notification;
import com.craftpilot.notificationservice.service.PushNotificationService;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.FirebaseMessagingException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Map;

@Slf4j
@Service
@ConditionalOnProperty(name = "firebase.enabled", havingValue = "true", matchIfMissing = false)
public class FirebasePushNotificationService implements PushNotificationService {

    private final FirebaseMessaging firebaseMessaging;
    private final Timer pushNotificationTimer;

    @Autowired
    public FirebasePushNotificationService(
            @Autowired(required = false) FirebaseMessaging firebaseMessaging, 
            @Autowired(required = false) Timer pushNotificationTimer) {
        this.firebaseMessaging = firebaseMessaging;
        this.pushNotificationTimer = pushNotificationTimer;
        
        if (firebaseMessaging == null) {
            log.warn("FirebaseMessaging is not available - push notifications will be logged but not sent");
        }
    }

    @Override
    @CircuitBreaker(name = "pushNotificationService")
    @Retry(name = "pushNotificationService")
    public Mono<Void> sendPushNotification(Notification notification) {
        return Mono.defer(() -> {
            Timer.Sample sample = Timer.start();
            
            try {
                // Notification data kontrolü
                if (notification.getData() == null) {
                    log.error("Notification data is null");
                    return Mono.error(new InvalidNotificationParametersException("Notification data cannot be null"));
                }
                
                Object deviceTokenObj = notification.getData().get("deviceToken");
                if (deviceTokenObj == null) {
                    log.error("Device token is missing in notification data");
                    return Mono.error(new InvalidNotificationParametersException("Device token is required"));
                }
                
                String deviceToken = deviceTokenObj.toString();
                if (deviceToken.isEmpty()) {
                    log.error("Device token is empty");
                    return Mono.error(new InvalidNotificationParametersException("Device token cannot be empty"));
                }
                
                // Firebase service unavailable - log and continue
                if (firebaseMessaging == null) {
                    log.info("Firebase messaging disabled or unavailable. Would have sent notification to device: {}, title: {}, content: {}",
                            deviceToken, notification.getTitle(), notification.getContent());
                    return Mono.empty();
                }
                
                Message message = Message.builder()
                        .setToken(deviceToken)
                        .setNotification(com.google.firebase.messaging.Notification.builder()
                                .setTitle(notification.getTitle())
                                .setBody(notification.getContent())
                                .build())
                        .putAllData(convertToStringMap(notification.getData()))
                        .build();

                try {
                    String messageId = firebaseMessaging.send(message);
                    if (pushNotificationTimer != null) {
                        sample.stop(pushNotificationTimer);
                    }
                    log.info("Push notification sent successfully to device: {}, messageId: {}", deviceToken, messageId);
                    return Mono.empty();
                } catch (FirebaseMessagingException e) {
                    log.error("Firebase bildirim gönderimi başarısız: {}", e.getMessage());
                    return Mono.error(new NotificationDeliveryException("Bildirim gönderilemedi: " + e.getMessage(), e));
                }
            } catch (IllegalArgumentException e) {
                log.error("Geçersiz bildirim parametreleri: {}", e.getMessage());
                return Mono.error(new InvalidNotificationParametersException("Geçersiz bildirim parametreleri: " + e.getMessage(), e));
            }
        });
    }

    private Map<String, String> convertToStringMap(Map<String, Object> data) {
        return data.entrySet().stream()
                .filter(entry -> entry.getValue() != null)
                .collect(java.util.stream.Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> String.valueOf(entry.getValue())
                ));
    }
}