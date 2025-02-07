package com.craftpilot.llmservice.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
@Configuration
public class FirestoreConfig {

    @Value("${spring.cloud.gcp.firestore.project-id:${GCP_PROJECT_ID:your-project-id}}")
    private String projectId;

    @Bean
    public Firestore firestore() throws IOException {
        GoogleCredentials credentials;
        String credentialsPath = System.getenv("GOOGLE_APPLICATION_CREDENTIALS");
        
        if (credentialsPath != null) {
            try (FileInputStream serviceAccountStream = new FileInputStream(credentialsPath)) {
                credentials = GoogleCredentials.fromStream(serviceAccountStream);
            }
        } else {
            credentials = GoogleCredentials.getApplicationDefault();
        }

        FirestoreOptions options = FirestoreOptions.newBuilder()
                .setProjectId(projectId)
                .setCredentials(credentials)
                .build();

        return options.getService();
    }
}