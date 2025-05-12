package com.craftpilot.activitylogservice.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.IOException;
import java.io.InputStream;

@Configuration
@Slf4j
public class FirestoreConfig {

    private final ResourceLoader resourceLoader;

    @Value("${GOOGLE_APPLICATION_CREDENTIALS:/app/credentials/gcp-credentials.json}")
    private String credentialsPath;

    @Value("${spring.cloud.gcp.project-id:craft-pilot-ai}")
    private String projectId;

    public FirestoreConfig(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Bean
    public Firestore firestore() throws IOException {
        try {
            log.info("Initializing Firestore configuration for project ID: {}", projectId);
            
            Resource resource = resourceLoader.getResource("file:" + credentialsPath);
            if (!resource.exists()) {
                throw new IOException("Credentials file not found at: " + credentialsPath);
            }

            GoogleCredentials credentials;
            try (InputStream inputStream = resource.getInputStream()) {
                credentials = GoogleCredentials.fromStream(inputStream);
            }

            FirestoreOptions firestoreOptions = FirestoreOptions.getDefaultInstance().toBuilder()
                .setProjectId(projectId)
                .setCredentials(credentials)
                .build();

            log.info("Firestore initialized successfully");
            return firestoreOptions.getService();
        } catch (IOException e) {
            log.error("Failed to initialize Firestore: {}", e.getMessage(), e);
            throw e;
        }
    }
}
