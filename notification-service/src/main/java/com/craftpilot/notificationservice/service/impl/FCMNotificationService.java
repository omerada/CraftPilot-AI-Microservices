package com.craftpilot.notificationservice.service.impl;

import com.google.firebase.messaging.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@ConditionalOnBean(FirebaseMessaging.class)
public class FCMNotificationService {

    private final FirebaseMessaging firebaseMessaging;

    @Autowired
    public FCMNotificationService(FirebaseMessaging firebaseMessaging) {
        this.firebaseMessaging = firebaseMessaging;
        if (firebaseMessaging == null) {
            log.warn("FirebaseMessaging is null - FCM notifications will not be available");
        }
    }

    public void sendNotification(String token, String title, String body) {
        if (firebaseMessaging == null) {
            log.info("FirebaseMessaging is not available. Would send notification - token: {}, title: {}, body: {}", token, title, body);
            return;
        }
        
        Message message = Message.builder()
            .setNotification(Notification.builder()
                .setTitle(title)
                .setBody(body)
                .build())
            .setToken(token)
            .build();

        try {
            String response = firebaseMessaging.send(message);
            log.info("Bildirim başarıyla gönderildi: {}", response);
        } catch (FirebaseMessagingException e) {
            log.error("Bildirim gönderilirken hata oluştu", e);
            throw new RuntimeException("Bildirim gönderilemedi", e);
        }
    }

    public void sendMulticastNotification(List<String> tokens, String title, String body) {
        if (firebaseMessaging == null) {
            log.info("FirebaseMessaging is not available. Would send multicast notification - tokens: {}, title: {}, body: {}", tokens, title, body);
            return;
        }
        
        MulticastMessage message = MulticastMessage.builder()
            .setNotification(Notification.builder()
                .setTitle(title)
                .setBody(body)
                .build())
            .addAllTokens(tokens)
            .build();

        try {
            BatchResponse response = firebaseMessaging.sendMulticast(message);
            log.info("Başarılı gönderimler: {}, Başarısız gönderimler: {}", 
                response.getSuccessCount(), response.getFailureCount());
        } catch (FirebaseMessagingException e) {
            log.error("Toplu bildirim gönderilirken hata oluştu", e);
            throw new RuntimeException("Toplu bildirim gönderilemedi", e);
        }
    }

    public void sendTopicNotification(String topic, String title, String body) {
        if (firebaseMessaging == null) {
            log.info("FirebaseMessaging is not available. Would send topic notification - topic: {}, title: {}, body: {}", topic, title, body);
            return;
        }
        
        Message message = Message.builder()
            .setNotification(Notification.builder()
                .setTitle(title)
                .setBody(body)
                .build())
            .setTopic(topic)
            .build();

        try {
            String response = firebaseMessaging.send(message);
            log.info("Topic bildirimi başarıyla gönderildi: {}", response);
        } catch (FirebaseMessagingException e) {
            log.error("Topic bildirimi gönderilirken hata oluştu", e);
            throw new RuntimeException("Topic bildirimi gönderilemedi", e);
        }
    }
}