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

    private final String projectId;
    private final ResourceLoader resourceLoader;
    private final String credentialsLocation;

    public FirestoreConfig(
            @Value("${spring.cloud.gcp.project-id:craft-pilot-ai}") String projectId,
            @Value("${spring.cloud.gcp.credentials.location:file:/etc/gcp/credentials/gcp-credentials.json}") String credentialsLocation,
            ResourceLoader resourceLoader) {
        this.projectId = projectId;
        this.resourceLoader = resourceLoader;
        this.credentialsLocation = credentialsLocation;
    }

    @Bean
    public Firestore firestore() throws IOException {
        log.info("Initializing Firestore with project ID: {} and credentials from: {}", projectId, credentialsLocation);
        
        GoogleCredentials credentials;
        
        try {
            Resource resource = resourceLoader.getResource(credentialsLocation);
            try (InputStream is = resource.getInputStream()) {
                credentials = GoogleCredentials.fromStream(is);
                log.info("Successfully loaded Google credentials from configured location");
            }
        } catch (IOException e) {
            log.warn("Failed to load credentials from {}: {}. Falling back to application default credentials.", 
                     credentialsLocation, e.getMessage());
            // Alternatif olarak uygulama varsayılan kimlik bilgilerini kullan
            credentials = GoogleCredentials.getApplicationDefault();
            log.info("Successfully loaded application default credentials");
        }

        FirestoreOptions firestoreOptions = FirestoreOptions.getDefaultInstance().toBuilder()
                .setProjectId(projectId)
                .setCredentials(credentials)
                .build();

        return firestoreOptions.getService();
    }
}
