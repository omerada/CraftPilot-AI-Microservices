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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

@Configuration
@Slf4j
public class FirestoreConfig {

    private final ResourceLoader resourceLoader;

    @Value("${GOOGLE_APPLICATION_CREDENTIALS:/etc/gcp/credentials/gcp-credentials.json}")
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
            log.info("Using credentials from path: {}", credentialsPath);
            
            GoogleCredentials credentials;
            try {
                // Direkt dosya yolu ile erişim
                credentials = GoogleCredentials.fromStream(new FileInputStream(credentialsPath));
                log.info("Successfully loaded credentials from file: {}", credentialsPath);
            } catch (IOException e) {
                log.warn("Could not load credentials directly, trying as resource: {}", e.getMessage());
                
                // Resource olarak erişim dene
                Resource resource = resourceLoader.getResource("file:" + credentialsPath);
                if (resource.exists()) {
                    try (InputStream inputStream = resource.getInputStream()) {
                        credentials = GoogleCredentials.fromStream(inputStream);
                        log.info("Successfully loaded credentials from resource");
                    }
                } else {
                    log.warn("Resource does not exist, falling back to application default credentials");
                    credentials = GoogleCredentials.getApplicationDefault();
                    log.info("Using application default credentials");
                }
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
