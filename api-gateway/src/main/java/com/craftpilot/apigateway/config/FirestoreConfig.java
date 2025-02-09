package com.craftpilot.apigateway.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.IOException;
import java.io.InputStream;

@Configuration
public class FirestoreConfig {
    private static final Logger logger = LoggerFactory.getLogger(FirestoreConfig.class);

    @Value("${gcp.credentials.path:/gcp-credentials.json}")
    private String credentialsPath;

    private final ResourceLoader resourceLoader;

    public FirestoreConfig(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Bean
    public Firestore firestore() throws IOException {
        try {
            Resource resource = resourceLoader.getResource(credentialsPath);
            InputStream serviceAccount = resource.getInputStream();
            
            GoogleCredentials credentials = GoogleCredentials.fromStream(serviceAccount);
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(credentials)
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
            }

            return FirestoreClient.getFirestore();
        } catch (Exception e) {
            logger.warn("Failed to initialize Firestore with credentials. Using default credentials: {}", e.getMessage());
            
            // Try to use default credentials (for local development or if running on GCP)
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.getApplicationDefault())
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
            }

            return FirestoreClient.getFirestore();
        }
    }
}