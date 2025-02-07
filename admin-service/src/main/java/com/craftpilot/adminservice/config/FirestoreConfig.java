package com.craftpilot.adminservice.config;
 
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.io.FileInputStream;
import java.io.IOException;

@Configuration
public class FirestoreConfig {
    private static final Logger logger = LoggerFactory.getLogger(FirestoreConfig.class);

    @Value("${GOOGLE_APPLICATION_CREDENTIALS}")
    private String credentialsPath;

    @Bean
    public Firestore firestore() throws IOException {
        try {
            logger.debug("Credentials path: {}", credentialsPath);
            
            GoogleCredentials credentials = GoogleCredentials.fromStream(
                new FileInputStream(credentialsPath)
            );

            FirestoreOptions firestoreOptions = FirestoreOptions.newBuilder()
                .setCredentials(credentials)
                .build();

            return firestoreOptions.getService();
        } catch (IllegalArgumentException e) {
            logger.error("Credentials path error: {}", e.getMessage());
            throw new IOException("Invalid credentials path", e);
        } catch (IOException e) {
            logger.error("Firestore configuration error: {}", e.getMessage());
            throw e;
        }
    }
} 