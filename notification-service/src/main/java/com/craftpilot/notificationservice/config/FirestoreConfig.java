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

    @Value("${spring.cloud.gcp.credentials.location}")
    private String credentialsPath;
    
    @Value("${spring.cloud.gcp.project-id}")
    private String projectId;

    @Bean
    public Firestore firestore() throws IOException {
        GoogleCredentials credentials;
        if (credentialsPath != null && credentialsPath.startsWith("file:")) {
            String path = credentialsPath.substring(5); // "file:" prefix'ini kaldır
            credentials = GoogleCredentials.fromStream(new FileInputStream(path));
        } else {
            // Default credentials
            credentials = GoogleCredentials.getApplicationDefault();
        }

        FirestoreOptions firestoreOptions = FirestoreOptions.getDefaultInstance().toBuilder()
                .setProjectId(projectId)
                .setCredentials(credentials)
                .build();

        return firestoreOptions.getService();
    }
} 