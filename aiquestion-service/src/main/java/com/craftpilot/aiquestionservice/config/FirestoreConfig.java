package com.craftpilot.aiquestionservice.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.IOException;

@Configuration
public class FirestoreConfig {

    @Value("${google.cloud.project-id}")
    private String projectId;

    @Value("${google.cloud.credentials.location}")
    private Resource credentialsLocation;

    @Bean
    public Firestore firestore() throws IOException {
        GoogleCredentials credentials = GoogleCredentials.fromStream(credentialsLocation.getInputStream());

        FirestoreOptions firestoreOptions = FirestoreOptions.getDefaultInstance().toBuilder()
                .setProjectId(projectId)
                .setCredentials(credentials)
                .build();

        return firestoreOptions.getService();
    }
} 