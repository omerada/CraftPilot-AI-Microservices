package com.craftpilot.notificationservice.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import lombok.extern.slf4j.Slf4j;

import java.io.FileInputStream;
import java.io.IOException;

@Configuration
@Slf4j
public class FirestoreConfig {

    @Value("${GCP_SA_KEY}")
    private String credentialsPath;
    
    @Value("${GCP_PROJECT_ID}")
    private String projectId;

    @Bean
    public Firestore firestore() throws IOException {
        log.info("Firebase kimlik bilgileri yükleniyor: {}", credentialsPath);
        GoogleCredentials credentials = GoogleCredentials.fromStream(
            new FileInputStream(credentialsPath));
        
        FirestoreOptions firestoreOptions = FirestoreOptions.newBuilder()
            .setCredentials(credentials)
            .setProjectId(projectId)
            .build();

        return firestoreOptions.getService();
    }
} 