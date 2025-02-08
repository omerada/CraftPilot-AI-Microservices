package com.craftpilot.notificationservice.service;

import com.craftpilot.notificationservice.model.EmailRequest;
import reactor.core.publisher.Mono;

public interface EmailService {
    Mono<Void> sendEmail(EmailRequest emailRequest);
}