package com.craftpilot.apigateway.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import com.google.firebase.auth.FirebaseAuth;
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
    private FirebaseApp firebaseApp;

    public FirestoreConfig(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    private FirebaseApp initializeFirebaseApp() throws IOException {
        if (firebaseApp != null) {
            return firebaseApp;
        }

        try {
            Resource resource = resourceLoader.getResource(credentialsPath);
            InputStream serviceAccount = resource.getInputStream();
            GoogleCredentials credentials = GoogleCredentials.fromStream(serviceAccount);
            
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(credentials)
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                firebaseApp = FirebaseApp.initializeApp(options);
            } else {
                firebaseApp = FirebaseApp.getInstance();
            }
            
            return firebaseApp;
        } catch (Exception e) {
            logger.warn("Failed to initialize Firebase with credentials. Using default credentials: {}", e.getMessage());
            
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.getApplicationDefault())
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                firebaseApp = FirebaseApp.initializeApp(options);
            } else {
                firebaseApp = FirebaseApp.getInstance();
            }
            
            return firebaseApp;
        }
    }

    @Bean
    public Firestore firestore() throws IOException {
        initializeFirebaseApp();
        return FirestoreClient.getFirestore();
    }

    @Bean
    public FirebaseAuth firebaseAuth() throws IOException {
        initializeFirebaseApp();
        return FirebaseAuth.getInstance();
    }
}