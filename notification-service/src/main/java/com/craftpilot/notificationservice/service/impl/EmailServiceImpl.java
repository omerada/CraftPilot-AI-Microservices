package com.craftpilot.notificationservice.service.impl;

import com.craftpilot.notificationservice.model.EmailRequest;
import com.craftpilot.notificationservice.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Override
    public Mono<Void> sendEmail(EmailRequest emailRequest) {
        return Mono.fromRunnable(() -> {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(emailRequest.getTo());
            message.setSubject(emailRequest.getSubject());
            message.setText(emailRequest.getContent());
            mailSender.send(message);
        })
        .subscribeOn(Schedulers.boundedElastic())
        .doOnSuccess(unused -> log.info("Email sent successfully to: {}", emailRequest.getTo()))
        .doOnError(error -> log.error("Failed to send email: {}", error.getMessage()))
        .then();
    }
}
