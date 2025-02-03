package com.craftpilot.notificationservice.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;

@Configuration
public class FirestoreConfig {

    @Value("${spring.cloud.gcp.project-id}")
    private String projectId;

    @Bean
    public Firestore firestore() throws IOException {
        ClassPathResource serviceAccount = new ClassPathResource("firebase-service-account.json");
        
        GoogleCredentials credentials = GoogleCredentials.fromStream(serviceAccount.getInputStream());
        
        FirestoreOptions firestoreOptions = FirestoreOptions.newBuilder()
                .setProjectId(projectId)
                .setCredentials(credentials)
                .build();
        
        return firestoreOptions.getService();
    }
} 