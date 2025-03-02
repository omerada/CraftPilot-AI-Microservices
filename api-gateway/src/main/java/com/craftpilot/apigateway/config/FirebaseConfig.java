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

import java.io.IOException;
import java.io.InputStream;

@Configuration
public class FirebaseConfig {
    private static final Logger log = LoggerFactory.getLogger(FirebaseConfig.class);
    
    @Value("${firebase.credentials-file:firebase-service-account.json}")
    private String credentialsPath;

    @Bean
    public FirebaseAuth firebaseAuth() throws IOException {
        if (FirebaseApp.getApps().isEmpty()) {
            Resource resource = new ClassPathResource(credentialsPath);
            try (InputStream serviceAccount = resource.getInputStream()) {
                FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();
                FirebaseApp.initializeApp(options);
            }
        }
        return FirebaseAuth.getInstance();
    }
}
