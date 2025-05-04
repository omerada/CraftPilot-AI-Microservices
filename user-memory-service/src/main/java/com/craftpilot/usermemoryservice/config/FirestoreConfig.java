package com.craftpilot.usermemoryservice.config;

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

    @Value("${spring.cloud.gcp.firestore.project-id:craft-pilot}")
    private String projectId;

    @Value("${spring.cloud.gcp.firestore.credentials.location:classpath:service-account.json}")
    private String credentialsLocation;

    private final ResourceLoader resourceLoader;

    public FirestoreConfig(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Bean
    public Firestore firestore() throws IOException {
        log.info("Initializing Firestore with project ID: {}", projectId);
        
        FirestoreOptions.Builder builder = FirestoreOptions.getDefaultInstance().toBuilder()
                .setProjectId(projectId);

        try {
            Resource resource = resourceLoader.getResource(credentialsLocation);
            if (resource.exists()) {
                log.info("Loading Firebase credentials from: {}", credentialsLocation);
                try (InputStream is = resource.getInputStream()) {
                    GoogleCredentials credentials = GoogleCredentials.fromStream(is);
                    builder.setCredentials(credentials);
                }
            } else {
                log.warn("Firebase credentials file not found at: {}. Using default application credentials.", credentialsLocation);
                // Default credentials (for local development or Google Cloud)
                GoogleCredentials credentials = GoogleCredentials.getApplicationDefault();
                builder.setCredentials(credentials);
            }
        } catch (IOException e) {
            log.error("Error loading Firebase credentials: {}", e.getMessage());
            log.warn("Falling back to application default credentials");
            GoogleCredentials credentials = GoogleCredentials.getApplicationDefault();
            builder.setCredentials(credentials);
        }

        return builder.build().getService();
    }
}
