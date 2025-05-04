package com.craftpilot.usermemoryservice.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;

@Configuration
@Slf4j
public class FirestoreConfig {

    @Value("${spring.cloud.gcp.project-id:craft-pilot-ai}")
    private String projectId;

    @Value("${spring.cloud.gcp.credentials.location:file:/etc/gcp/credentials/gcp-credentials.json}")
    private String credentialsPath;

    @Bean
    public Firestore firestore() throws IOException {
        String resolvedPath = credentialsPath.replace("file:", "");
        log.info("Initializing Firestore with project ID: {} and credentials at: {}", projectId, resolvedPath);
        
        GoogleCredentials credentials;
        try (FileInputStream serviceAccountStream = new FileInputStream(resolvedPath)) {
            credentials = GoogleCredentials.fromStream(serviceAccountStream);
        }
        
        FirestoreOptions firestoreOptions = FirestoreOptions.getDefaultInstance().toBuilder()
                .setProjectId(projectId)
                .setCredentials(credentials)
                .build();
        
        log.info("Firestore initialized successfully");
        return firestoreOptions.getService();
    }
}
