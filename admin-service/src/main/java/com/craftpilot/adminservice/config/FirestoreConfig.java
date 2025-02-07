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

    @Value("${GOOGLE_APPLICATION_CREDENTIALS:#{null}}")
    private String credentialsPath;

    @Value("${GCP_PROJECT_ID:craft-pilot-ai}")
    private String projectId;

    @Bean
    public Firestore firestore() throws IOException {
        FirestoreOptions.Builder builder = FirestoreOptions.getDefaultInstance().toBuilder()
            .setProjectId(projectId);

        if (credentialsPath != null) {
            GoogleCredentials credentials = GoogleCredentials.fromStream(
                new FileInputStream(credentialsPath));
            builder.setCredentials(credentials);
        }

        return builder.build().getService();
    }
} 