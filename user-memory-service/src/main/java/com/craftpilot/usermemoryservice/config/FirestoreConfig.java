package com.craftpilot.usermemoryservice.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@Configuration
@Slf4j
public class FirestoreConfig {

    @Value("${firestore.project-id:craft-pilot}")
    private String projectId;

    @Bean
    public Firestore firestore() throws IOException {
        GoogleCredentials credentials;
        
        // Check for credential file in /etc/gcp/credentials
        if (Files.exists(Paths.get("/etc/gcp/credentials/gcp-credentials.json"))) {
            log.info("Loading GCP credentials from /etc/gcp/credentials/gcp-credentials.json");
            credentials = GoogleCredentials.fromStream(
                Files.newInputStream(Paths.get("/etc/gcp/credentials/gcp-credentials.json"))
            );
        } 
        // Try environment variable path
        else if (System.getenv("GOOGLE_APPLICATION_CREDENTIALS") != null) {
            log.info("Loading GCP credentials from GOOGLE_APPLICATION_CREDENTIALS: {}", 
                    System.getenv("GOOGLE_APPLICATION_CREDENTIALS"));
            credentials = GoogleCredentials.fromStream(
                Files.newInputStream(Paths.get(System.getenv("GOOGLE_APPLICATION_CREDENTIALS")))
            );
        }
        // Default credentials as fallback
        else {
            log.info("No explicit credentials file found, using application default credentials");
            credentials = GoogleCredentials.getApplicationDefault();
        }

        FirestoreOptions firestoreOptions = FirestoreOptions.newBuilder()
                .setProjectId(projectId)
                .setCredentials(credentials)
                .build();
        
        log.info("Firestore configured successfully with project ID: {}", projectId);
        return firestoreOptions.getService();
    }
}
