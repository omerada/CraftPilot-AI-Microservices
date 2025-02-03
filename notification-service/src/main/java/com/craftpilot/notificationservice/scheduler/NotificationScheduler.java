package com.craftpilot.notificationservice.scheduler;

import com.craftpilot.notificationservice.model.Notification;
import com.craftpilot.notificationservice.service.EmailService;
import com.craftpilot.notificationservice.service.NotificationService;
import com.craftpilot.notificationservice.service.PushNotificationService;
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
    private final EmailService emailService;
    private final PushNotificationService pushNotificationService;
    private final MeterRegistry meterRegistry;

    @Scheduled(cron = "${notification.scheduler.cron:0 */5 * * * *}")
    @CircuitBreaker(name = "processScheduledNotifications")
    public void processScheduledNotifications() {
        log.info("Starting scheduled notification processing");
        
        notificationService.getScheduledNotifications()
                .flatMap(notification -> sendNotification(notification)
                        .then(markNotificationAsSent(notification))
                        .onErrorResume(error -> {
                            log.error("Failed to process notification: {}", notification.getId(), error);
                            meterRegistry.counter("notification.scheduled.failed").increment();
                            return Mono.empty();
                        }))
                .doOnComplete(() -> {
                    log.info("Completed scheduled notification processing");
                    meterRegistry.counter("notification.scheduled.processed").increment();
                })
                .subscribe();
    }

    private Mono<Void> sendNotification(Notification notification) {
        return switch (notification.getType()) {
            case EMAIL -> emailService.sendEmail(notification);
            case PUSH -> pushNotificationService.sendPushNotification(notification);
            default -> {
                log.warn("Unsupported notification type: {}", notification.getType());
                yield Mono.empty();
            }
        };
    }

    private Mono<Void> markNotificationAsSent(Notification notification) {
        notification.setSentAt(LocalDateTime.now());
        return notificationService.markAsSent(notification.getId());
    }
} 