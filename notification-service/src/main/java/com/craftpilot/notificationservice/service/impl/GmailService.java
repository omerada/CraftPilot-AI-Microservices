package com.craftpilot.notificationservice.service.impl;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.Properties;

@Service
@Slf4j
public class GmailService {

    @Value("${GCP_SA_KEY}")
    private String credentialsPath;

    @Value("${spring.mail.from}")
    private String fromEmail;

    private final Gmail gmail;

    public GmailService() throws GeneralSecurityException, IOException {
        GoogleCredentials credentials = GoogleCredentials.fromStream(new FileInputStream(credentialsPath))
                .createScoped(Collections.singletonList(
                    "https://www.googleapis.com/auth/gmail.send"
                ));

        gmail = new Gmail.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                new HttpCredentialsAdapter(credentials))
                .setApplicationName("Craft Pilot AI")
                .build();
    }

    public Mono<Void> sendEmail(String to, String subject, String body) {
        return Mono.fromCallable(() -> {
            try {
                Message message = createMessageWithEmail(createEmail(to, subject, body));
                String response = gmail.users().messages().send("me", message).execute().getId();
                log.info("E-posta başarıyla gönderildi: {} - Message ID: {}", to, response);
                return null;
            } catch (Exception e) {
                log.error("E-posta gönderilirken hata oluştu: {}", e.getMessage());
                throw new RuntimeException("E-posta gönderilemedi: " + e.getMessage(), e);
            }
        }).then();
    }

    private MimeMessage createEmail(String to, String subject, String body) throws MessagingException {
        Properties props = new Properties();
        Session session = Session.getDefaultInstance(props, null);

        MimeMessage email = new MimeMessage(session);
        email.setFrom(new InternetAddress(fromEmail));
        email.addRecipient(jakarta.mail.Message.RecipientType.TO, new InternetAddress(to));
        email.setSubject(subject);
        email.setText(body);

        return email;
    }

    private Message createMessageWithEmail(MimeMessage email) throws MessagingException, IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        email.writeTo(buffer);
        byte[] bytes = buffer.toByteArray();
        String encodedEmail = org.apache.commons.codec.binary.Base64.encodeBase64URLSafeString(bytes);
        Message message = new Message();
        message.setRaw(encodedEmail);
        return message;
    }
} 