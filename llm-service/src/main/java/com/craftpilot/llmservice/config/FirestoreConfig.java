package com.craftpilot.llmservice.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.io.IOException;
import java.io.InputStream;

@Configuration
public class FirestoreConfig {

    @Value("${firestore.credential.path:/gcp-credentials.json}")
    private String credentialPath;

    @Bean
    @Primary
    public Firestore firestore() throws IOException {
        try {
            // Google Cloud kimlik bilgilerini yükle
            InputStream serviceAccount = getClass().getResourceAsStream(credentialPath);
            if (serviceAccount == null) {
                // Eğer resource olarak bulunamazsa, dosya sisteminden yüklemeyi dene
                java.nio.file.Path path = java.nio.file.Paths.get(credentialPath);
                if (java.nio.file.Files.exists(path)) {
                    serviceAccount = java.nio.file.Files.newInputStream(path);
                    System.out.println("Google Cloud kimliği için ortam değişkeni kullanılıyor: " + credentialPath);
                } else {
                    throw new IOException("Credentials file not found: " + credentialPath);
                }
            }

            GoogleCredentials credentials = GoogleCredentials.fromStream(serviceAccount);
            System.out.println("Google Cloud kimliği başarıyla yüklendi: " + credentialPath);

            // Firestore yapılandırması
            FirestoreOptions firestoreOptions = FirestoreOptions.getDefaultInstance().toBuilder()
                    .setCredentials(credentials)
                    .build();

            // Firestore instance'ını döndür
            return firestoreOptions.getService();
        } catch (IOException e) {
            System.err.println("Firestore yapılandırması oluşturulamadı: " + e.getMessage());
            throw e;
        }
    }
}