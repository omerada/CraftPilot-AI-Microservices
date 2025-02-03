package com.craftpilot.notificationservice.service.impl;

import com.craftpilot.notificationservice.model.Notification;
import com.craftpilot.notificationservice.service.EmailService;
import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class SendGridEmailService implements EmailService {

    private final SendGrid sendGrid;
    private final Timer emailSendingTimer;

    @Value("${spring.mail.from}")
    private String fromEmail;

    @Override
    @CircuitBreaker(name = "emailService")
    @Retry(name = "emailService")
    public Mono<Void> sendEmail(Notification notification) {
        return Mono.defer(() -> {
            Timer.Sample sample = Timer.start();
            
            try {
                Email from = new Email(fromEmail);
                Email to = new Email(notification.getData().get("email").toString());
                Content content = new Content("text/html", notification.getContent());
                Mail mail = new Mail(from, notification.getTitle(), to, content);

                Request request = new Request();
                request.setMethod(Method.POST);
                request.setEndpoint("mail/send");
                request.setBody(mail.build());

                return Mono.fromCallable(() -> sendGrid.api(request))
                        .doOnSuccess(response -> {
                            sample.stop(emailSendingTimer);
                            if (response.getStatusCode() >= 400) {
                                throw new RuntimeException("Failed to send email: " + response.getBody());
                            }
                            log.info("Email sent successfully to {}", to.getEmail());
                        })
                        .doOnError(e -> log.error("Error sending email to {}: {}", to.getEmail(), e.getMessage()))
                        .then();
            } catch (Exception e) {
                return Mono.error(e);
            }
        });
    }
} 