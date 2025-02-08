package com.craftpilot.notificationservice.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@Configuration
@Slf4j
public class FirestoreConfig {

    @Value("${spring.cloud.gcp.project-id}")
    private String projectId;

    @Value("${GCP_SA_KEY:}")
    private String gcpServiceAccountKey;

    @Bean
    public Firestore firestore() throws IOException {
        GoogleCredentials credentials;
        
        // Önce environment variable'dan JSON string'i kontrol et
        if (gcpServiceAccountKey != null && !gcpServiceAccountKey.isEmpty()) {
            log.info("GCP kimlik bilgileri environment variable'dan yükleniyor");
            credentials = GoogleCredentials.fromStream(
                new ByteArrayInputStream(gcpServiceAccountKey.getBytes())
            );
        }
        // Dosya sisteminden okumayı dene
        else if (Files.exists(Paths.get("/gcp-credentials.json"))) {
            log.info("GCP kimlik bilgileri /gcp-credentials.json dosyasından yükleniyor");
            credentials = GoogleCredentials.fromStream(
                Files.newInputStream(Paths.get("/gcp-credentials.json"))
            );
        }
        // GOOGLE_APPLICATION_CREDENTIALS environment variable'ını kontrol et
        else if (System.getenv("GOOGLE_APPLICATION_CREDENTIALS") != null) {
            log.info("GCP kimlik bilgileri GOOGLE_APPLICATION_CREDENTIALS'dan yükleniyor");
            credentials = GoogleCredentials.fromStream(
                Files.newInputStream(Paths.get(System.getenv("GOOGLE_APPLICATION_CREDENTIALS")))
            );
        }
        // Son çare olarak varsayılan kimlik bilgilerini kullan
        else {
            log.info("GCP varsayılan kimlik bilgileri kullanılıyor");
            credentials = GoogleCredentials.getApplicationDefault();
        }

        FirestoreOptions firestoreOptions = FirestoreOptions.newBuilder()
                .setProjectId(projectId)
                .setCredentials(credentials)
                .build();
        
        return firestoreOptions.getService();
    }
} 