package com.craftpilot.notificationservice.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@ConditionalOnProperty(name = "firebase.enabled", havingValue = "true", matchIfMissing = false)
public class FirebaseConfig {

    @Value("${firebase.credentials-file:}")
    private String credentialsPath;
    
    @Value("${firebase.credentials-classpath:}")
    private String credentialsClasspath;

    @Bean
    public FirebaseMessaging firebaseMessaging() {
        if (FirebaseApp.getApps().isEmpty()) {
            try {
                InputStream serviceAccount = getCredentialsInputStream();
                if (serviceAccount == null) {
                    log.warn("Firebase credentials not found. Firebase messaging is disabled.");
                    return null;
                }
                
                FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();
                FirebaseApp.initializeApp(options);
                log.info("Firebase application has been initialized successfully");
            } catch (IOException e) {
                log.error("Failed to initialize Firebase: {}", e.getMessage());
                log.warn("Application will continue without Firebase messaging capabilities");
                return null;
            } catch (Exception e) {
                log.error("Unexpected error initializing Firebase: {}", e.getMessage());
                log.warn("Application will continue without Firebase messaging capabilities");
                return null;
            }
        }
        
        try {
            return FirebaseMessaging.getInstance();
        } catch (Exception e) {
            log.error("Could not get FirebaseMessaging instance: {}", e.getMessage());
            return null;
        }
    }
    
    private InputStream getCredentialsInputStream() {
        // İlk olarak classpath'den okumayı dene
        if (StringUtils.hasText(credentialsClasspath)) {
            try {
                Resource resource = new ClassPathResource(credentialsClasspath);
                if (resource.exists()) {
                    log.info("Loading Firebase credentials from classpath: {}", credentialsClasspath);
                    return resource.getInputStream();
                }
            } catch (Exception e) {
                log.warn("Could not load Firebase credentials from classpath: {}", e.getMessage());
            }
        }
        
        // Dosya sisteminden okumayı dene
        if (StringUtils.hasText(credentialsPath)) {
            try {
                log.info("Loading Firebase credentials from file: {}", credentialsPath);
                return new FileInputStream(credentialsPath);
            } catch (IOException e) {
                log.warn("Could not load Firebase credentials from file: {}", e.getMessage());
            }
        }
        
        log.warn("No Firebase credentials configuration found");
        return null;
    }
}
