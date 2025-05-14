package com.craftpilot.notificationservice.scheduler;

import com.craftpilot.notificationservice.model.EmailRequest;
import com.craftpilot.notificationservice.model.Notification;
import com.craftpilot.notificationservice.service.NotificationService;
import com.craftpilot.notificationservice.service.PushNotificationService;
import com.craftpilot.notificationservice.service.impl.SendGridService;
import com.craftpilot.notificationservice.service.EmailService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationScheduler {

    private final NotificationService notificationService;
    private final PushNotificationService pushNotificationService;
    private final SendGridService sendGridService;
    private final MeterRegistry meterRegistry;
    private final EmailService emailService;

    @Scheduled(fixedRate = 60000)
    @CircuitBreaker(name = "processScheduledNotifications")
    public void processScheduledNotifications() {
        LocalDateTime now = LocalDateTime.now();

        notificationService.getScheduledNotifications()
                .filter(notification -> notification.getScheduledAt().isBefore(now))
                .flatMap(this::processAndUpdateNotification)
                .subscribe();
    }

    private Mono<Notification> processAndUpdateNotification(Notification notification) {
        try {
            sendGridService.sendEmail(
                    notification.getRecipientEmail(),
                    notification.getTitle(),
                    notification.getContent());
            notification.setProcessed(true);
            notification.setProcessedTime(LocalDateTime.now());
            return notificationService.saveNotification(notification);
        } catch (Exception e) {
            log.error("Failed to process notification: {}", notification.getId(), e);
            return Mono.empty();
        }
    }

    private Mono<Void> sendNotification(Notification notification) {
        return switch (notification.getType()) {
            case EMAIL -> {
                processEmailNotification(notification);
                yield Mono.empty();
            }
            case PUSH -> pushNotificationService.sendPushNotification(notification);
            default -> {
                log.warn("Unsupported notification type: {}", notification.getType());
                yield Mono.empty();
            }
        };
    }

    private void processEmailNotification(Notification notification) {
        EmailRequest emailRequest = EmailRequest.builder()
                .to(notification.getRecipient())
                .subject(notification.getSubject())
                .content(notification.getContent())
                .build();

        emailService.sendEmail(emailRequest)
                .doOnSuccess(
                        unused -> log.info("Scheduled email sent successfully to: {}", notification.getRecipient()))
                .doOnError(
                        error -> log.error("Failed to send scheduled email to: {}", notification.getRecipient(), error))
                .subscribe();
    }

    private Mono<Void> markNotificationAsSent(Notification notification) {
        notification.setSentAt(LocalDateTime.now());
        return notificationService.markAsSent(notification.getId());
    }
}