package com.craftpilot.subscriptionservice.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@Configuration
public class FirestoreConfig {

    @Value("${spring.cloud.gcp.project-id}")
    private String projectId;

    @Bean
    public Firestore firestore() throws IOException {
        log.info("Initializing Firestore with project ID: {}", projectId);
        
        try {
            // Önce container içindeki credentials dosyasını kontrol et
            Path credentialsPath = Paths.get("/gcp-credentials/credentials.json");
            GoogleCredentials credentials;
            
            if (Files.exists(credentialsPath)) {
                log.info("Using GCP credentials from mounted volume");
                try (InputStream serviceAccount = new FileInputStream(credentialsPath.toFile())) {
                    credentials = GoogleCredentials.fromStream(serviceAccount);
                }
            } else {
                // Environment variable'dan credentials'ı al
                String gcpCredentials = System.getenv("GOOGLE_APPLICATION_CREDENTIALS");
                if (gcpCredentials != null && !gcpCredentials.isEmpty()) {
                    log.info("Using GCP credentials from environment variable");
                    try (InputStream serviceAccount = new ByteArrayInputStream(gcpCredentials.getBytes())) {
                        credentials = GoogleCredentials.fromStream(serviceAccount);
                    }
                } else {
                    log.error("No GCP credentials found");
                    throw new IOException("GCP credentials not found");
                }
            }

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