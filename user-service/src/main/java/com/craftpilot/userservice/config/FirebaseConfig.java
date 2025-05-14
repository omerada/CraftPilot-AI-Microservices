package com.craftpilot.userservice.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import javax.annotation.PostConstruct;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Slf4j
@Configuration
public class FirebaseConfig {

    @Value("${firebase.config.path:/etc/gcp/credentials/gcp-credentials.json}")
    private String firebaseConfigPath;

    private final ResourceLoader resourceLoader;

    public FirebaseConfig(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @PostConstruct
    public void initializeFirebase() {
        try {
            // Firebase'in zaten başlatılıp başlatılmadığını kontrol et
            if (FirebaseApp.getApps().isEmpty()) {
                log.info("Firebase'i {} yapılandırma dosyası ile başlatılıyor...", firebaseConfigPath);

                InputStream serviceAccount;
                Resource resource = resourceLoader.getResource("file:" + firebaseConfigPath);

                if (resource.exists()) {
                    serviceAccount = resource.getInputStream();
                } else {
                    log.warn("Firebase yapılandırma dosyası bulunamadı: {}", firebaseConfigPath);
                    log.info("Sınıf yolu üzerinde bir yapılandırma dosyası aranıyor...");

                    // Alternatif olarak classpath'ten yükle
                    serviceAccount = getClass().getResourceAsStream("/firebase-config.json");

                    if (serviceAccount == null) {
                        log.error("Firebase yapılandırma dosyası bulunamadı");
                        throw new IOException("Firebase yapılandırma dosyası bulunamadı");
                    }
                }

                // Firestore'u devre dışı bırakarak sadece Authentication için yapılandırma
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .setDatabaseUrl(null) // Veritabanı kullanımını devre dışı bırak
                        .build();

                FirebaseApp.initializeApp(options);
                log.info("Firebase başlatıldı");
            } else {
                log.info("Firebase zaten başlatılmış");
            }
        } catch (IOException e) {
            log.error("Firebase başlatma hatası: {}", e.getMessage(), e);
            throw new RuntimeException("Firebase başlatma hatası", e);
        }
    }

    @Bean
    public FirebaseAuth firebaseAuth() {
        return FirebaseAuth.getInstance();
    }
}
