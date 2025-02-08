package com.craftpilot.notificationservice.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.FileInputStream;
import java.io.IOException;

@Configuration
public class FirestoreConfig {

    @Value("${spring.cloud.gcp.project-id}")
    private String projectId;

    @Value("${google.credentials.path:/app/config/gcp-credentials.json}")
    private String credentialsPath;

    @Bean
    public Firestore firestore() throws IOException {
        GoogleCredentials credentials;
        try {
            credentials = GoogleCredentials.fromStream(new FileInputStream(credentialsPath));
        } catch (IOException e) {
            throw new IllegalStateException("GCP kimlik bilgileri yüklenemedi: " + e.getMessage(), e);
        }
        
        FirestoreOptions firestoreOptions = FirestoreOptions.newBuilder()
                .setProjectId(projectId)
                .setCredentials(credentials)
                .build();
        
        return firestoreOptions.getService();
    }
} 