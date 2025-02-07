package com.craftpilot.adminservice.config;
 
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;

@Configuration
public class FirestoreConfig {

    @Value("${spring.cloud.gcp.credentials.location}")
    private String credentialsPath;

    @Value("${spring.cloud.gcp.firestore.project-id}")
    private String projectId;

    @Bean
    public Firestore firestore() throws IOException {
        String cleanPath = credentialsPath.replace("file:", "");
        GoogleCredentials credentials = GoogleCredentials.fromStream(
            new FileInputStream(cleanPath)
        );

        FirestoreOptions firestoreOptions = FirestoreOptions.newBuilder()
            .setProjectId(projectId)
            .setCredentials(credentials)
            .build();

        return firestoreOptions.getService();
    }
} 