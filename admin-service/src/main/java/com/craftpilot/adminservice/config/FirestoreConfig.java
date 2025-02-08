package com.craftpilot.adminservice.config;
 
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.IOException;
import java.io.InputStream;

@Configuration
public class FirestoreConfig {
    private static final Logger logger = LoggerFactory.getLogger(FirestoreConfig.class);

    private final ResourceLoader resourceLoader;

    @Value("${GOOGLE_APPLICATION_CREDENTIALS}")
    private String credentialsPath;

    public FirestoreConfig(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Bean
    public Firestore firestore() throws IOException {
        try {
            logger.info("Firestore yapılandırması başlatılıyor. Kimlik dosyası yolu: {}", credentialsPath);
            
            Resource resource = resourceLoader.getResource("file:" + credentialsPath);
            if (!resource.exists()) {
                throw new IOException("Kimlik dosyası bulunamadı: " + credentialsPath);
            }

            GoogleCredentials credentials;
            try (InputStream inputStream = resource.getInputStream()) {
                credentials = GoogleCredentials.fromStream(inputStream);
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