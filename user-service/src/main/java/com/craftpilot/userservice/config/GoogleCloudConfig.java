package com.craftpilot.userservice.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import reactor.core.publisher.Mono;

import java.io.IOException;

@Configuration
public class GoogleCloudConfig {

    @Value("${spring.cloud.gcp.project-id}")
    private String projectId;

    private final Environment environment;

    public GoogleCloudConfig(Environment environment) {
        this.environment = environment;
    }

    @Bean
    public GoogleCredentials googleCredentials() throws IOException {
        if (environment.acceptsProfiles(profiles -> profiles.test())) {
            return GoogleCredentials.create(null);
        }
        return GoogleCredentials.getApplicationDefault();
    }

    @Bean
    public FirebaseApp firebaseApp(GoogleCredentials credentials) {
        FirebaseOptions options = FirebaseOptions.builder()
                .setProjectId(projectId)
                .setCredentials(credentials)
                .build();

        return FirebaseApp.getApps().isEmpty() 
                ? FirebaseApp.initializeApp(options)
                : FirebaseApp.getInstance();
    }

    @Bean
    public Firestore firestore(GoogleCredentials credentials) {
        FirestoreOptions options = FirestoreOptions.newBuilder()
                .setProjectId(projectId)
                .setCredentials(credentials)
                .build();

        return options.getService();
    }

    @Bean
    public FirebaseAuth firebaseAuth(FirebaseApp firebaseApp) {
        return FirebaseAuth.getInstance(firebaseApp);
    }

    @Bean
    public Mono<FirebaseAuth> reactiveFirebaseAuth(FirebaseAuth firebaseAuth) {
        return Mono.just(firebaseAuth).cache();
    }
}
