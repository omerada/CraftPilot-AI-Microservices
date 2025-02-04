package com.craftpilot.analyticsservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Configuration
public class FirestoreConfig {

    @Value("${GCP_SA_KEY}")
    private String gcpServiceAccountKey;

    @Bean
    public Firestore firestore() throws IOException {
        // Google Cloud Default Credentials kullan
        GoogleCredentials credentials = GoogleCredentials.getApplicationDefault();
        
        FirestoreOptions firestoreOptions = FirestoreOptions.newBuilder()
            .setProjectId("craft-pilot-ai")
            .setCredentials(credentials)
            .build();

        return firestoreOptions.getService();
    }
} 