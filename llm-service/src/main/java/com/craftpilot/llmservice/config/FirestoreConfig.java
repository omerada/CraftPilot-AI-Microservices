package com.craftpilot.llmservice.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
@Configuration
public class FirestoreConfig {

    @Value("${GCP_SA_KEY}")
    private String gcpCredentials;

    @Bean
    public Firestore firestore() throws IOException {
        try {
            // GCP kimlik bilgilerini JSON'dan parse et
            Gson gson = new GsonBuilder().setLenient().create();
            String formattedJson = gson.toJson(gson.fromJson(gcpCredentials, Object.class));
            
            // Kimlik bilgilerini InputStream'e dönüştür
            ByteArrayInputStream credentialsStream = new ByteArrayInputStream(
                formattedJson.getBytes(StandardCharsets.UTF_8)
            );

            // Google kimlik bilgilerini oluştur
            GoogleCredentials credentials = GoogleCredentials.fromStream(credentialsStream);

            // Firestore yapılandırmasını oluştur
            FirestoreOptions firestoreOptions = FirestoreOptions.newBuilder()
                .setCredentials(credentials)
                .build();

            log.info("Firestore başarıyla yapılandırıldı");
            return firestoreOptions.getService();
        } catch (Exception e) {
            log.error("Firestore yapılandırması sırasında hata: ", e);
            throw e;
        }
    }
}