package com.craftpilot.imageservice.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@Configuration
public class FirestoreConfig {

    @Value("${spring.cloud.gcp.project-id}")
    private String projectId;

    @Value("${spring.cloud.gcp.credentials.location}")
    private String credentialsPath;

    @Bean
    public Firestore firestore() throws IOException {
        GoogleCredentials credentials;
        
        // Önce dosya sisteminden okumayı dene
        if (Files.exists(Paths.get("/gcp-credentials.json"))) {
            credentials = GoogleCredentials.fromStream(
                Files.newInputStream(Paths.get("/gcp-credentials.json"))
            );
        } 
        // Dosya bulunamazsa environment variable'dan oku
        else if (System.getenv("GOOGLE_APPLICATION_CREDENTIALS") != null) {
            credentials = GoogleCredentials.fromStream(
                Files.newInputStream(Paths.get(System.getenv("GOOGLE_APPLICATION_CREDENTIALS")))
            );
        }
        // Son çare olarak klasik yolu dene
        else {
            credentials = GoogleCredentials.getApplicationDefault();
        }

        FirestoreOptions firestoreOptions = FirestoreOptions.newBuilder()
                .setProjectId(projectId)
                .setCredentials(credentials)
                .build();
        
        return firestoreOptions.getService();
    }
} 