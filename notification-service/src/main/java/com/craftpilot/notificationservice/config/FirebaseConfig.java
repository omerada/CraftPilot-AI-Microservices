package com.craftpilot.notificationservice.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.messaging.FirebaseMessaging;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
import java.util.Optional;

@Configuration
public class FirebaseConfig {
    private static final Logger logger = LoggerFactory.getLogger(FirebaseConfig.class);

    // Update to support both env variables with fallback path
    @Value("${firebase.credentials.path:${FIREBASE_CONFIG:${GOOGLE_APPLICATION_CREDENTIALS:/app/credentials/firebase-credentials.json}}}")
    private String credentialsPath;
    
    @Value("${firebase.credentials.alternate-paths:/app/gcp-credentials.json,/craftpilot/gcp-credentials.json,/gcp-credentials.json,/app/config/firebase-credentials.json}")
    private String alternativeCredentialsPaths;
    
    @Value("${firebase.enabled:true}")
    private boolean firebaseEnabled;

    private final ResourceLoader resourceLoader;
    private boolean initializationSuccessful = false;

    public FirebaseConfig(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @PostConstruct
    public void initialize() {
        try {
            if (!firebaseEnabled) {
                logger.info("Firebase is disabled by configuration. Skipping initialization.");
                return;
            }
            
            if (FirebaseApp.getApps().isEmpty()) {
                logger.info("Initializing Firebase with credentials");
                
                Optional<InputStream> credentialsStream = loadCredentials();
                if (credentialsStream.isPresent()) {
                    try {
                        initializeFirebase(credentialsStream.get());
                        initializationSuccessful = true;
                    } catch (Exception e) {
                        logger.error("Firebase initialization failed: {}", e.getMessage(), e);
                    } finally {
                        credentialsStream.get().close();
                    }
                } else {
                    logger.warn("Failed to load Firebase credentials from any source. Firebase features will be disabled.");
                }
            } else {
                logger.info("Firebase already initialized, skipping initialization");
                initializationSuccessful = true;
            }
        } catch (Exception e) {
            logger.error("Failed during Firebase configuration: {}", e.getMessage(), e);
        }
    }
    
    private Optional<InputStream> loadCredentials() {
        logger.info("Attempting to load Firebase credentials");
        
        // Check environment variables first
        String envFirebaseConfig = System.getenv("FIREBASE_CONFIG");
        String envGoogleCreds = System.getenv("GOOGLE_APPLICATION_CREDENTIALS");
        
        logger.info("Environment variables: FIREBASE_CONFIG={}, GOOGLE_APPLICATION_CREDENTIALS={}", 
                envFirebaseConfig != null ? "set" : "not set", 
                envGoogleCreds != null ? "set" : "not set");
        
        // 1. Try from FIREBASE_CONFIG environment variable if set
        if (envFirebaseConfig != null && !envFirebaseConfig.isEmpty()) {
            try {
                logger.info("Attempting to load from FIREBASE_CONFIG environment variable: {}", envFirebaseConfig);
                FileInputStream serviceAccount = new FileInputStream(envFirebaseConfig);
                return Optional.of(serviceAccount);
            } catch (IOException e) {
                logger.warn("Could not load credentials from FIREBASE_CONFIG: {}", e.getMessage());
            }
        }
        
        // 2. Try from GOOGLE_APPLICATION_CREDENTIALS environment variable if set
        if (envGoogleCreds != null && !envGoogleCreds.isEmpty()) {
            try {
                logger.info("Attempting to load from GOOGLE_APPLICATION_CREDENTIALS: {}", envGoogleCreds);
                FileInputStream serviceAccount = new FileInputStream(envGoogleCreds);
                return Optional.of(serviceAccount);
            } catch (IOException e) {
                logger.warn("Could not load credentials from GOOGLE_APPLICATION_CREDENTIALS: {}", e.getMessage());
            }
        }
        
        // 3. Try primary path from configuration
        try {
            logger.info("Attempting to load from primary path: {}", credentialsPath);
            FileInputStream serviceAccount = new FileInputStream(credentialsPath);
            return Optional.of(serviceAccount);
        } catch (IOException e) {
            logger.info("Could not load credentials from primary path: {}", e.getMessage());
        }
        
        // 4. Try alternate paths
        List<String> altPaths = Arrays.asList(alternativeCredentialsPaths.split(","));
        for (String path : altPaths) {
            try {
                logger.info("Attempting to load from alternate path: {}", path);
                FileInputStream serviceAccount = new FileInputStream(path);
                return Optional.of(serviceAccount);
            } catch (IOException e) {
                logger.info("Could not load credentials from alternate path {}: {}", path, e.getMessage());
            }
        }
        
        // 5. Try as resource
        try {
            logger.info("Attempting to load credentials as resource");
            Resource resource = resourceLoader.getResource("classpath:firebase-credentials.json");
            if (resource.exists()) {
                return Optional.of(resource.getInputStream());
            }
        } catch (IOException e) {
            logger.info("Could not load credentials as resource: {}", e.getMessage());
        }
        
        logger.error("Could not load Firebase credentials from any location");
        return Optional.empty();
    }
    
    private void initializeFirebase(InputStream credentialsStream) throws IOException {
        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(credentialsStream))
                .build();
        FirebaseApp.initializeApp(options);
        logger.info("Firebase initialization successful");
    }

    @Bean
    @ConditionalOnProperty(name = "firebase.enabled", havingValue = "true", matchIfMissing = true)
    public FirebaseAuth firebaseAuth() {
        if (!initializationSuccessful) {
            logger.warn("Firebase initialization was not successful, but returning a default FirebaseAuth instance");
        }
        return FirebaseAuth.getInstance();
    }
    
    @Bean
    @ConditionalOnProperty(name = "firebase.enabled", havingValue = "true", matchIfMissing = true)
    public FirebaseMessaging firebaseMessaging() {
        if (!initializationSuccessful) {
            logger.warn("Firebase initialization was not successful, returning null FirebaseMessaging");
            return null;
        }
        return FirebaseMessaging.getInstance();
    }
}
