package com.craftpilot.userservice.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;

@Configuration
public class FirestoreConfig {

    @Value("${spring.cloud.gcp.project-id}")
    private String projectId;

    @Bean
    public Firestore firestore() throws IOException {
        if (System.getProperty("FIRESTORE_EMULATOR_HOST") != null) {
            // Test ortamı için
            return FirestoreOptions.newBuilder()
                    .setProjectId("test-project")
                    .setHost(System.getProperty("FIRESTORE_EMULATOR_HOST"))
                    .setCredentials(GoogleCredentials.create(null))
                    .build()
                    .getService();
        } else {
            // Production ortamı için
            ClassPathResource serviceAccount = new ClassPathResource("firebase-service-account.json");
            GoogleCredentials credentials = GoogleCredentials.fromStream(serviceAccount.getInputStream());
            
            return FirestoreOptions.newBuilder()
                    .setProjectId(projectId)
                    .setCredentials(credentials)
                    .build()
                    .getService();
        }
    }
} 