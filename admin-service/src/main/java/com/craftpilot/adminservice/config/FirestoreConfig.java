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
import java.nio.file.Files;
import java.nio.file.Paths;

@Configuration
public class FirestoreConfig {
    private static final Logger logger = LoggerFactory.getLogger(FirestoreConfig.class);

    @Value("${GOOGLE_APPLICATION_CREDENTIALS}")
    private String credentialsPath;

    @Bean
    public Firestore firestore() throws IOException {
        try {
            logger.info("Firestore yapılandırması başlatılıyor. Kimlik dosyası yolu: {}", credentialsPath);
            
            // Dosyanın varlığını ve içeriğini kontrol et
            if (!Files.exists(Paths.get(credentialsPath))) {
                throw new IOException("Kimlik dosyası bulunamadı: " + credentialsPath);
            }

            String jsonContent = new String(Files.readAllBytes(Paths.get(credentialsPath)));
            if (jsonContent.trim().isEmpty()) {
                throw new IOException("Kimlik dosyası boş: " + credentialsPath);
            }

            logger.debug("Kimlik dosyası içeriği doğrulandı");
            
            GoogleCredentials credentials;
            try (FileInputStream serviceAccountStream = new FileInputStream(credentialsPath)) {
                credentials = GoogleCredentials.fromStream(serviceAccountStream);
            }

            FirestoreOptions firestoreOptions = FirestoreOptions.newBuilder()
                .setCredentials(credentials)
                .build();

            logger.info("Firestore başarıyla yapılandırıldı");
            return firestoreOptions.getService();
        } catch (IOException e) {
            logger.error("Firestore yapılandırması başarısız oldu: {}", e.getMessage(), e);
            throw new RuntimeException("Firestore yapılandırması başarısız oldu: " + e.getMessage(), e);
        }
    }
} 