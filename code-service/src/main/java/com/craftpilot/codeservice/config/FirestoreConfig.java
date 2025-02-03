package com.craftpilot.codeservice.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.util.StreamUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

@Configuration
public class FirestoreConfig {
    @Value("${firebase.credential.path}")
    private String credentialPath;

    @Value("${firebase.project.id}")
    private String projectId;

    @Bean
    public Firestore firestore() throws IOException {
        Resource resource = new ClassPathResource(credentialPath);
        byte[] credentialBytes = StreamUtils.copyToByteArray(resource.getInputStream());
        InputStream serviceAccount = new ByteArrayInputStream(credentialBytes);

        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .setProjectId(projectId)
                .build();

        if (FirebaseApp.getApps().isEmpty()) {
            FirebaseApp.initializeApp(options);
        } else {
            FirebaseApp.getInstance();
        }

        return FirestoreClient.getFirestore();
    }
} 