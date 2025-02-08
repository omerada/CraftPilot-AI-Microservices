package com.craftpilot.notificationservice.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration; 
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Configuration
@Slf4j
public class FirestoreConfig {

    @Value("${GOOGLE_APPLICATION_CREDENTIALS}")
    private String credentialsPath;

    @Bean
    public Firestore firestore() throws IOException {
        log.info("Firebase kimlik bilgileri yükleniyor: {}", credentialsPath);
        GoogleCredentials credentials = GoogleCredentials.getApplicationDefault();
        
        FirestoreOptions firestoreOptions = FirestoreOptions.newBuilder()
            .setCredentials(credentials)
            .build();

        return firestoreOptions.getService();
    }
} 