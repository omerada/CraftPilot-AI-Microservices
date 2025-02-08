package com.craftpilot.notificationservice.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import lombok.extern.slf4j.Slf4j;

import java.io.FileInputStream;
import java.io.IOException;

@Configuration
@Slf4j
public class FirebaseConfig {

    @Value("${spring.cloud.gcp.credentials.location}")
    private String credentialsPath;
    
    @Value("${spring.cloud.gcp.project-id}")
    private String projectId;

    @Bean
    @Primary
    public GoogleCredentials googleCredentials() throws IOException {
        log.info("Firebase/Firestore kimlik bilgileri yükleniyor: {}", credentialsPath);
        String actualPath = credentialsPath.replace("file:", "");
        return GoogleCredentials.fromStream(new FileInputStream(actualPath));
    }

    @Bean
    public FirebaseApp firebaseApp(GoogleCredentials credentials) throws IOException {
        if (FirebaseApp.getApps().isEmpty()) {
            FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(credentials)
                .setProjectId(projectId)
                .build();

            return FirebaseApp.initializeApp(options);
        }
        return FirebaseApp.getInstance();
    }

    @Bean
    public FirebaseMessaging firebaseMessaging(FirebaseApp firebaseApp) {
        return FirebaseMessaging.getInstance(firebaseApp);
    }

    @Bean
    public Firestore firestore(GoogleCredentials credentials) {
        FirestoreOptions firestoreOptions = FirestoreOptions.newBuilder()
            .setCredentials(credentials)
            .setProjectId(projectId)
            .build();

        return firestoreOptions.getService();
    }
} 