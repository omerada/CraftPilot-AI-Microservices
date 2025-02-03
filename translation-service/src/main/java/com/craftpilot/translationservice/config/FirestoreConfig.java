package com.craftpilot.translationservice.config;

import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;

@Configuration
@ConditionalOnProperty(name = "firebase.enabled", havingValue = "true", matchIfMissing = false)
public class FirestoreConfig {

    @Value("${firebase.credential.path}")
    private String credentialPath;

    @Value("${firebase.project.id}")
    private String projectId;

    @Bean
    public Firestore firestore() throws IOException {
        if (FirebaseApp.getApps().isEmpty()) {
            Resource resource = new ClassPathResource(credentialPath);
            InputStream serviceAccount = resource.getInputStream();
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(com.google.auth.oauth2.GoogleCredentials.fromStream(serviceAccount))
                    .setProjectId(projectId)
                    .build();
            FirebaseApp.initializeApp(options);
        }
        return FirestoreClient.getFirestore();
    }
} 