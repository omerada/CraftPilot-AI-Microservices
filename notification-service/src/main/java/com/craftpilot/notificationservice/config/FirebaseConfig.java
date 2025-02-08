package com.craftpilot.notificationservice.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Configuration
@Slf4j
public class FirebaseConfig {

    @Value("${GOOGLE_APPLICATION_CREDENTIALS}")
    private String credentialsPath;

    @Bean
    public FirebaseMessaging firebaseMessaging() throws IOException {
        log.info("Firebase kimlik bilgileri yükleniyor: {}", credentialsPath);
        GoogleCredentials credentials = GoogleCredentials.getApplicationDefault();
        
        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(credentials)
                .build();
        
        if (FirebaseApp.getApps().isEmpty()) {
            FirebaseApp.initializeApp(options);
        }
        
        return FirebaseMessaging.getInstance();
    }
} 