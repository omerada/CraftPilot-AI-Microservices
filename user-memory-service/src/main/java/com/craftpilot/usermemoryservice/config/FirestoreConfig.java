package com.craftpilot.usermemoryservice.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class FirestoreConfig {

    @Value("${firebase.credentials-path:/etc/gcp/credentials/gcp-credentials.json}")
    private String firebaseConfigPath;
    
    @Value("${spring.cloud.gcp.project-id:craft-pilot-ai}")
    private String projectId;

    @PostConstruct
    public void initialize() {
        try {
            log.info("Initializing Firestore with project ID: {} and credentials from: {}", projectId, firebaseConfigPath);
            if (FirebaseApp.getApps().isEmpty()) {
                FileSystemResource serviceAccount = new FileSystemResource(firebaseConfigPath);
                
                try {
                    FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount.getInputStream()))
                        .build();

                    FirebaseApp.initializeApp(options);
                } catch (IOException e) {
                    log.warn("Failed to load credentials from {}: {}. Falling back to application default credentials.", firebaseConfigPath, e.getMessage());
                    FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.getApplicationDefault())
                        .build();

                    FirebaseApp.initializeApp(options);
                }
            }
        } catch (IOException e) {
            log.error("Failed to initialize Firebase", e);
            throw new RuntimeException("Failed to initialize Firebase", e);
        }
    }

    @Bean
    public FirebaseAuth firebaseAuth() {
        return FirebaseAuth.getInstance();
    }
    
    @Bean
    public Firestore firestore() throws IOException {
        try {
            FileSystemResource serviceAccount = new FileSystemResource(firebaseConfigPath);
            GoogleCredentials credentials = GoogleCredentials.fromStream(serviceAccount.getInputStream());
            
            return FirestoreOptions.getDefaultInstance().toBuilder()
                .setProjectId(projectId)
                .setCredentials(credentials)
                .build()
                .getService();
        } catch (IOException e) {
            log.warn("Failed to load credentials from {}: {}. Falling back to application default credentials.", firebaseConfigPath, e.getMessage());
            GoogleCredentials credentials = GoogleCredentials.getApplicationDefault();
            
            return FirestoreOptions.getDefaultInstance().toBuilder()
                .setProjectId(projectId)
                .setCredentials(credentials)
                .build()
                .getService();
        }
    }
}
