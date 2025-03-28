package com.craftpilot.llmservice.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

@Configuration
@Slf4j
public class FirestoreConfig {

    @Bean
    public Firestore firestore() throws IOException {
        // Önce çevreden servis hesabı kimlik dosyasını almayı deneyin
        String credentialsPath = System.getenv("GOOGLE_APPLICATION_CREDENTIALS");
        GoogleCredentials credentials;

        if (credentialsPath != null && !credentialsPath.isEmpty()) {
            log.info("Google Cloud kimliği için ortam değişkeni kullanılıyor: {}", credentialsPath);
            try {
                // Çevresel değişken ayarlanmışsa, doğrudan belirtilen dosyadan oku
                if (Files.exists(Paths.get(credentialsPath))) {
                    try (FileInputStream fileInputStream = new FileInputStream(credentialsPath)) {
                        credentials = GoogleCredentials.fromStream(fileInputStream);
                        log.info("Google Cloud kimliği başarıyla yüklendi: {}", credentialsPath);
                    }
                } else {
                    log.warn("Belirtilen kimlik dosyası bulunamadı: {}, varsayılan kimlik bilgileri kullanılacak", credentialsPath);
                    credentials = GoogleCredentials.getApplicationDefault();
                }
            } catch (IOException e) {
                log.error("Kimlik dosyası okunurken hata oluştu: {}", e.getMessage());
                // Dosya okunamazsa varsayılan kimlik bilgilerini kullan
                credentials = GoogleCredentials.getApplicationDefault();
            }
        } else {
            log.info("GOOGLE_APPLICATION_CREDENTIALS ortam değişkeni tanımlanmamış, classpath'ten yüklemeye çalışılıyor");
            // Veya projedeki servis hesabı anahtarını kullanın (geliştirme için)
            try {
                Resource resource = new ClassPathResource("serviceAccountKey.json");
                InputStream serviceAccount = resource.getInputStream();
                credentials = GoogleCredentials.fromStream(serviceAccount);
                log.info("Servis hesabı anahtarı classpath'ten başarıyla yüklendi");
            } catch (Exception e) {
                log.warn("Classpath'ten yükleme başarısız, varsayılan kimlik bilgileri kullanılacak: {}", e.getMessage());
                // Son çare olarak varsayılan kimlik bilgilerini almaya çalışın
                credentials = GoogleCredentials.getApplicationDefault();
            }
        }

        FirestoreOptions firestoreOptions = FirestoreOptions.getDefaultInstance().toBuilder()
                .setCredentials(credentials)
                .build();

        return firestoreOptions.getService();
    }
}