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

    @Value("${spring.cloud.gcp.firestore.project-id}")
    private String projectId;

    @Value("${spring.cloud.gcp.credentials.location}")
    private String credentialsPath;

    @Bean
    public Firestore firestore() throws IOException {
        FileInputStream serviceAccount = new FileInputStream(credentialsPath.substring(5));
        GoogleCredentials credentials = GoogleCredentials.fromStream(serviceAccount);

        FirestoreOptions firestoreOptions =
                FirestoreOptions.getDefaultInstance().toBuilder()
                        .setProjectId(projectId)
                        .setCredentials(credentials)
                        .build();

        return firestoreOptions.getService();
    }
} 