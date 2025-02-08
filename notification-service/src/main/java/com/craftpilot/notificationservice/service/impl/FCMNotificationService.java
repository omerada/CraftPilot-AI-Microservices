package com.craftpilot.notificationservice.service.impl;

import com.google.firebase.messaging.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FCMNotificationService {

    private final FirebaseMessaging firebaseMessaging;

    public void sendNotification(String token, String title, String body) {
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