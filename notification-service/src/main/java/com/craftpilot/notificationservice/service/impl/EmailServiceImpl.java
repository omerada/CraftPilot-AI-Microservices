package com.craftpilot.notificationservice.service.impl;

import com.craftpilot.notificationservice.model.EmailRequest;
import com.craftpilot.notificationservice.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {
    
    private final JavaMailSender mailSender;
    
    @Value("${notification.email.from}")
    private String fromEmail;

    @Override
    public Mono<Void> sendEmail(EmailRequest emailRequest) {
        return Mono.fromCallable(() -> {
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true);
                
                helper.setFrom(fromEmail);
                helper.setTo(emailRequest.getTo());
                helper.setSubject(emailRequest.getSubject());
                helper.setText(emailRequest.getContent(), true);
                
                mailSender.send(message);
                log.info("Email sent successfully to: {}", emailRequest.getTo());
                return null;
            } catch (MessagingException e) {
                log.error("Failed to send email", e);
                throw new RuntimeException("Failed to send email", e);
            }
        })
        .subscribeOn(Schedulers.boundedElastic())
        .then();
    }
}
