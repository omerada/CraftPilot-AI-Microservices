package com.craftpilot.notificationservice.service.impl;

import com.craftpilot.notificationservice.dto.NotificationRequest;
import com.craftpilot.notificationservice.dto.NotificationResponse;
import com.craftpilot.notificationservice.model.EmailRequest;
import com.craftpilot.notificationservice.model.Notification;
import com.craftpilot.notificationservice.model.NotificationTemplate;
import com.craftpilot.notificationservice.repository.NotificationRepository;
import com.craftpilot.notificationservice.service.NotificationService;
import com.craftpilot.notificationservice.service.NotificationTemplateService;
import com.craftpilot.notificationservice.service.PushNotificationService;
import com.craftpilot.notificationservice.service.EmailService;
import com.craftpilot.notificationservice.model.enums.NotificationType;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationTemplateService templateService;
    private final PushNotificationService pushNotificationService;
    private final MeterRegistry meterRegistry;
    private final SendGridService sendGridService;
    private final EmailService emailService;

    @Override
    @CircuitBreaker(name = "createNotification")
    @Retry(name = "createNotification")
    public Mono<NotificationResponse> createNotification(NotificationRequest request) {
        return templateService.getTemplate(request.getTemplateId())
                .flatMap(template -> createNotificationFromTemplate(request, template))
                .flatMap(notification -> {
                    if (notification.getScheduledAt() != null && 
                            notification.getScheduledAt().isAfter(LocalDateTime.now())) {
                        return saveNotification(notification).map(NotificationResponse::fromEntity);
                    }
                    return sendNotification(notification)
                            .then(saveNotification(notification))
                            .map(NotificationResponse::fromEntity);
                })
                .doOnSuccess(response -> {
                    meterRegistry.counter("notification.created").increment();
                    log.info("Notification created successfully: {}", response.getId());
                })
                .doOnError(error -> {
                    meterRegistry.counter("notification.creation.failed").increment();
                    log.error("Failed to create notification", error);
                });
    }

    @Override
    public Mono<NotificationResponse> getNotification(String id) {
        return notificationRepository.findById(id)
                .map(NotificationResponse::fromEntity)
                .doOnSuccess(response -> log.debug("Retrieved notification: {}", id));
    }

    @Override
    public Flux<NotificationResponse> getUserNotifications(String userId, boolean onlyUnread) {
        return (onlyUnread ? 
                notificationRepository.findByUserIdAndRead(userId, false) :
                notificationRepository.findByUserId(userId))
                .map(NotificationResponse::fromEntity)
                .doOnComplete(() -> log.debug("Retrieved notifications for user: {}", userId));
    }

    @Override
    public Mono<NotificationResponse> markAsRead(String id) {
        return notificationRepository.findById(id)
                .flatMap(notification -> {
                    notification.setRead(true);
                    notification.setUpdatedAt(LocalDateTime.now());
                    return notificationRepository.save(notification);
                })
                .map(NotificationResponse::fromEntity)
                .doOnSuccess(response -> log.debug("Marked notification as read: {}", id));
    }

    @Override
    public Mono<Void> deleteNotification(String id) {
        return notificationRepository.deleteById(id)
                .doOnSuccess(unused -> {
                    meterRegistry.counter("notification.deleted").increment();
                    log.info("Deleted notification: {}", id);
                });
    }

    @Override
    public Flux<Notification> getScheduledNotifications() {
        return notificationRepository.findByScheduledAtAfter(LocalDateTime.now())
                .doOnError(e -> log.error("Error fetching scheduled notifications: {}", e.getMessage()))
                .onErrorResume(e -> {
                    log.error("Falling back to empty flux for scheduled notifications", e);
                    return Flux.empty();
                });
    }

    @Override
    public Mono<Void> markAsSent(String id) {
        return notificationRepository.findById(id)
                .flatMap(notification -> {
                    notification.setSentAt(LocalDateTime.now());
                    notification.setUpdatedAt(LocalDateTime.now());
                    return notificationRepository.save(notification);
                })
                .then()
                .doOnSuccess(unused -> log.debug("Marked notification as sent: {}", id));
    }

    private Mono<Notification> createNotificationFromTemplate(NotificationRequest request, 
            NotificationTemplate template) {
        Notification notification = new Notification();
        notification.setUserId(request.getUserId());
        notification.setTemplateId(template.getId());
        notification.setType(NotificationType.valueOf(request.getType()));
        notification.setTitle(processTemplate(template.getTitleTemplate(), request.getVariables()));
        notification.setContent(processTemplate(template.getContentTemplate(), request.getVariables()));
        notification.setData(request.getAdditionalData());
        notification.setScheduledAt(request.getScheduledAt());
        notification.setCreatedAt(LocalDateTime.now());
        notification.setRead(false);
        return Mono.just(notification);
    }

    private String processTemplate(String template, java.util.Map<String, Object> variables) {
        String result = template;
        if (variables != null) {
            for (java.util.Map.Entry<String, Object> entry : variables.entrySet()) {
                result = result.replace("${" + entry.getKey() + "}", 
                        String.valueOf(entry.getValue()));
            }
        }
        return result;
    }

    private Mono<Void> sendNotification(Notification notification) {
        return switch (notification.getType()) {
            case EMAIL -> sendEmail(notification);
            case PUSH -> pushNotificationService.sendPushNotification(notification);
            default -> {
                log.warn("Unsupported notification type: {}", notification.getType());
                yield Mono.empty();
            }
        };
    }

    @Override
    public Mono<Notification> saveNotification(Notification notification) {
        return notificationRepository.save(notification)
                .doOnSuccess(saved -> log.debug("Saved notification: {}", saved.getId()))
                .doOnError(error -> log.error("Error saving notification: {}", error.getMessage()));
    }

    private Mono<Void> sendEmail(Notification notification) {
        EmailRequest emailRequest = EmailRequest.builder()
            .to(notification.getRecipient())
            .subject(notification.getSubject())
            .content(notification.getContent())
            .build();
            
        return emailService.sendEmail(emailRequest)
                .doOnSuccess(v -> log.info("Email sent successfully to: {}", notification.getRecipient()))
                .doOnError(e -> log.error("Failed to send email to: {}", notification.getRecipient(), e));
    }

    private void sendEmailNotification(Notification notification) {
        try {
            sendGridService.sendEmail(
                notification.getRecipientEmail(),
                notification.getTitle(),
                notification.getBody()
            );
            notification.setProcessed(true);
            notification.setProcessedTime(LocalDateTime.now());
            notificationRepository.save(notification).subscribe();
        } catch (Exception e) {
            log.error("Failed to send email notification: {}", e.getMessage());
        }
    }
}