package com.craftpilot.notificationservice.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import javax.annotation.PostConstruct;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

@Configuration
public class FirebaseConfig {
    private static final Logger logger = LoggerFactory.getLogger(FirebaseConfig.class);

    // Update to check multiple credential paths in priority order
    @Value("${firebase.credentials.path:${FIREBASE_CONFIG:/app/credentials/firebase-credentials.json}}")
    private String credentialsPath;
    
    @Value("${firebase.credentials.alternate-paths:/app/gcp-credentials.json,/craftpilot/gcp-credentials.json,/gcp-credentials.json}")
    private String alternativeCredentialsPaths;

    private final ResourceLoader resourceLoader;

    public FirebaseConfig(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @PostConstruct
    public void initialize() {
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                logger.info("Initializing Firebase with credentials");
                
                InputStream credentialsStream = loadCredentials();
                if (credentialsStream != null) {
                    initializeFirebase(credentialsStream);
                } else {
                    throw new IOException("Failed to load Firebase credentials from any source");
                }
            } else {
                logger.info("Firebase already initialized, skipping initialization");
            }
        } catch (Exception e) {
            logger.error("Failed to initialize Firebase: {}", e.getMessage(), e);
            throw new RuntimeException("Firebase initialization failed", e);
        }
    }
    
    private InputStream loadCredentials() throws IOException {
        // 1. First try primary path
        try {
            logger.info("Attempting to load from primary path: {}", credentialsPath);
            FileInputStream serviceAccount = new FileInputStream(credentialsPath);
            return serviceAccount;
        } catch (IOException e) {
            logger.info("Could not load credentials from primary path: {}", e.getMessage());
        }
        
        // 2. Try alternate paths
        List<String> altPaths = Arrays.asList(alternativeCredentialsPaths.split(","));
        for (String path : altPaths) {
            try {
                logger.info("Attempting to load from alternate path: {}", path);
                FileInputStream serviceAccount = new FileInputStream(path);
                return serviceAccount;
            } catch (IOException e) {
                logger.info("Could not load credentials from alternate path {}: {}", path, e.getMessage());
            }
        }
        
        // 3. Try as resource
        try {
            logger.info("Attempting to load credentials as resource");
            Resource resource = resourceLoader.getResource("classpath:firebase-credentials.json");
            if (resource.exists()) {
                return resource.getInputStream();
            }
        } catch (IOException e) {
            logger.info("Could not load credentials as resource: {}", e.getMessage());
        }
        
        logger.error("Could not load Firebase credentials from any location");
        return null;
    }
    
    private void initializeFirebase(InputStream credentialsStream) throws IOException {
        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(credentialsStream))
                .build();
        FirebaseApp.initializeApp(options);
        logger.info("Firebase initialization successful");
    }

    @Bean
    public FirebaseAuth firebaseAuth() {
        return FirebaseAuth.getInstance();
    }
}
