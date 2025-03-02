package com.craftpilot.apigateway.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;

@Configuration
public class FirebaseConfig {
    private static final Logger log = LoggerFactory.getLogger(FirebaseConfig.class);

    @Value("${spring.firebase.credentials.path:/app/config/firebase-credentials.json}")
    private String credentialsPath;

    private final ResourceLoader resourceLoader;

    public FirebaseConfig(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @PostConstruct
    public void initialize() {
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                log.info("Initializing Firebase with credentials from: {}", credentialsPath);
                
                Resource resource = resourceLoader.getResource("file:" + credentialsPath);
                if (!resource.exists()) {
                    log.error("Firebase credentials file not found at: {}", credentialsPath);
                    throw new IOException("Firebase credentials file not found");
                }

                try (InputStream serviceAccount = resource.getInputStream()) {
                    FirebaseOptions options = FirebaseOptions.builder()
                            .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                            .build();
                    FirebaseApp.initializeApp(options);
                    log.info("Firebase initialization successful");
                }
            }
        } catch (IOException e) {
            log.error("Failed to initialize Firebase. Path: {}. Error: {}", credentialsPath, e.getMessage(), e);
            throw new RuntimeException("Firebase initialization failed: " + e.getMessage(), e);
        }
    }

    @Bean
    public FirebaseAuth firebaseAuth() {
        // FirebaseApp.getApps() null olabilir, bu durumu kontrol et
        if (FirebaseApp.getApps() == null || FirebaseApp.getApps().isEmpty()) {
            try {
                log.info("Initializing Firebase Auth in firebaseAuth() bean");
                
                // Önce dosyanın varlığını kontrol et
                Resource resource = resourceLoader.getResource("file:" + credentialsPath);
                if (!resource.exists()) {
                    resource = new ClassPathResource("firebase-service-account.json");
                    if (!resource.exists()) {
                        throw new IOException("Firebase credentials file not found at: " + credentialsPath);
                    }
                    log.info("Using firebase-service-account.json from classpath");
                }
                
                try (InputStream serviceAccount = resource.getInputStream()) {
                    FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .build();
                    
                    FirebaseApp app = FirebaseApp.initializeApp(options);
                    log.info("Firebase application has been initialized: {}", app.getName());
                }
            } catch (IOException e) {
                log.error("Error initializing Firebase application: {}", e.getMessage(), e);
                throw new IllegalStateException("Firebase initialization failed: " + e.getMessage(), e);
            }
        }
        
        log.info("Returning FirebaseAuth instance");
        return FirebaseAuth.getInstance();
    }
}
