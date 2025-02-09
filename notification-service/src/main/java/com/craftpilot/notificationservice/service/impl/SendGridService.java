package com.craftpilot.notificationservice.service.impl;

import com.sendgrid.*;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@Slf4j
@RequiredArgsConstructor
public class SendGridService {
    
    private final SendGrid sendGrid;
    
    @Value("${spring.mail.sendgrid.from}")
    private String fromEmail;

    public void sendEmail(String to, String subject, String body) throws IOException {
        Email from = new Email(fromEmail);
        Email toEmail = new Email(to);
        Content content = new Content("text/html", body);
        Mail mail = new Mail(from, subject, toEmail, content);

        Request request = new Request();
        request.setMethod(Method.POST);
        request.setEndpoint("mail/send");
        request.setBody(mail.build());

        Response response = sendGrid.api(request);
        if (response.getStatusCode() != 202) {
            log.error("SendGrid error: {} - {}", response.getStatusCode(), response.getBody());
            throw new IOException("Failed to send email through SendGrid");
        }
    }
}
