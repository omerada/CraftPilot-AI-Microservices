package com.craftpilot.userservice.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import javax.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Arrays;

@Configuration
@ConditionalOnProperty(name = "firebase.enabled", havingValue = "true", matchIfMissing = true)
public class FirebaseConfig {
    private static final Logger logger = LoggerFactory.getLogger(FirebaseConfig.class);

    @Value("${firebase.credentials.path:/gcp/credentials/gcp-credentials.json}")
    private String credentialsPath;
    
    @Value("${firebase.credentials.alternate-paths:/craftpilot/gcp-credentials.json,/app/gcp-credentials.json}")
    private String alternativeCredentialsPaths;
    
    @Value("${firebase.credentials.base64:#{null}}")
    private String credentialsBase64;

    @Value("${firebase.required:false}")
    private boolean firebaseRequired;

    private final ResourceLoader resourceLoader;

    public FirebaseConfig(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @PostConstruct
    public void initialize() {
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                initializeFirebase();
            }
        } catch (Exception e) {
            String message = "Firebase initialization failed: " + e.getMessage();
            logger.error(message, e);
            
            // Sadece gerekli ise hata fırlat, aksi takdirde uyarı ver ve devam et
            if (firebaseRequired) {
                throw new RuntimeException(message, e);
            } else {
                logger.warn("Firebase is not required for this deployment, continuing without Firebase integration");
            }
        }
    }

    private void initializeFirebase() throws IOException {
        InputStream serviceAccount = loadCredentials();
        if (serviceAccount == null) {
            throw new IOException("Failed to load Firebase credentials from any source");
        }

        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .build();
        
        FirebaseApp.initializeApp(options);
        logger.info("Firebase initialization successful");
    }
    
    private InputStream loadCredentials() throws IOException {
        // 1. Önce çevre değişkeninden yüklemeye çalış
        if (credentialsBase64 != null && !credentialsBase64.isEmpty()) {
            logger.info("Attempting to initialize Firebase with credentials from environment variable");
            try {
                byte[] decodedBytes = Base64.getDecoder().decode(credentialsBase64);
                return new ByteArrayInputStream(decodedBytes);
            } catch (IllegalArgumentException e) {
                logger.warn("Invalid Base64 format for Firebase credentials", e);
            }
        }
        
        // 2. Ana dosya konumundan yüklemeye çalış
        try {
            logger.info("Attempting to initialize Firebase with credentials from primary path: {}", credentialsPath);
            File credentialsFile = new File(credentialsPath);
            if (credentialsFile.exists() && credentialsFile.canRead()) {
                return new FileInputStream(credentialsFile);
            }
        } catch (IOException e) {
            logger.warn("Could not load Firebase credentials from primary path: {}", credentialsPath);
        }
        
        // 3. Alternatif konumlardan yüklemeye çalış
        List<String> paths = Arrays.asList(alternativeCredentialsPaths.split(","));
        for (String path : paths) {
            try {
                logger.info("Attempting to initialize Firebase with credentials from alternative path: {}", path);
                File credentialsFile = new File(path);
                if (credentialsFile.exists() && credentialsFile.canRead()) {
                    return new FileInputStream(credentialsFile);
                }
            } catch (IOException e) {
                logger.warn("Could not load Firebase credentials from alternative path: {}", path);
            }
        }
        
        // 4. Classpath içinden yüklemeye çalış
        try {
            logger.info("Attempting to initialize Firebase with credentials from classpath");
            Resource resource = resourceLoader.getResource("classpath:firebase-credentials.json");
            if (resource.exists()) {
                return resource.getInputStream();
            }
        } catch (IOException e) {
            logger.warn("Could not load Firebase credentials from classpath", e);
        }
        
        return null;
    }

    @Bean
    public FirebaseAuth firebaseAuth() {
        // FirebaseApp zaten başlatılmışsa, FirebaseAuth'u döndürür
        // Başlatılmamışsa ve Firebase zorunlu değilse null döndürür
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                if (firebaseRequired) {
                    throw new IllegalStateException("Firebase is required but not initialized");
                }
                logger.warn("Returning null FirebaseAuth as Firebase is not initialized");
                return null;
            }
            return FirebaseAuth.getInstance();
        } catch (Exception e) {
            if (firebaseRequired) {
                throw e;
            }
            logger.warn("Error getting FirebaseAuth instance: {}", e.getMessage());
            return null;
        }
    }
}
