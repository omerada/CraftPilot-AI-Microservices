package com.craftpilot.creditservice.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;

@Configuration
public class FirestoreConfig {

    @Bean
    public Firestore firestore() throws IOException {
        GoogleCredentials credentials = GoogleCredentials.fromStream(
                new ClassPathResource("firebase-service-account.json").getInputStream());

        FirestoreOptions options = FirestoreOptions.newBuilder()
                .setCredentials(credentials)
                .build();

        return options.getService();
    }
} 