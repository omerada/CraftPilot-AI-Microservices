package com.craftpilot.subscriptionservice.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import lombok.extern.slf4j.Slf4j;

import java.io.FileInputStream;
import java.io.IOException;

@Slf4j
@Configuration
public class FirestoreConfig {

    @Value("${spring.cloud.gcp.project-id}")
    private String projectId;

    @Value("${spring.cloud.gcp.credentials.location}")
    private String credentialsPath;

    @Bean
    public Firestore firestore() throws IOException {
        log.info("Initializing Firestore with project ID: {}", projectId);
        
        try {
            String resolvedPath = credentialsPath.replace("file:", "");
            log.info("Loading credentials from: {}", resolvedPath);
            
            GoogleCredentials credentials = GoogleCredentials.fromStream(
                new FileInputStream(resolvedPath));

            FirestoreOptions firestoreOptions = FirestoreOptions.getDefaultInstance()
                    .toBuilder()
                    .setProjectId(projectId)
                    .setCredentials(credentials)
                    .build();
            
            log.info("Successfully initialized Firestore configuration");
            return firestoreOptions.getService();
        } catch (IOException e) {
            log.error("Failed to initialize Firestore: {}", e.getMessage(), e);
            throw e;
        }
    }
} 