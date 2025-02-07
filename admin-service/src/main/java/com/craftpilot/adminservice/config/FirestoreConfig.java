package com.craftpilot.adminservice.config;
 
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
        // Önce credentials.json dosyasını classpath'ten yüklemeyi dene
        GoogleCredentials credentials;
        try {
            credentials = GoogleCredentials.fromStream(
                new ClassPathResource("firebase-credentials.json").getInputStream()
            );
        } catch (IOException e) {
            // Eğer dosya bulunamazsa, varsayılan credentials'ı kullan
            credentials = GoogleCredentials.getApplicationDefault();
        }

        FirestoreOptions firestoreOptions = FirestoreOptions.newBuilder()
            .setProjectId(projectId)
            .setCredentials(credentials)
            .build();

        return firestoreOptions.getService();
    }
} 