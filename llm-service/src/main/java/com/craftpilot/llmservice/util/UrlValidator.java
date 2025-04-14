package com.craftpilot.llmservice.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

@Component
@Slf4j
public class UrlValidator {
    private static final List<String> VALID_SCHEMES = Arrays.asList("http", "https");
    
    public void validate(String urlString) {
        try {
            URL url = new URL(urlString);
            
            // Protokolü kontrol et
            if (!VALID_SCHEMES.contains(url.getProtocol())) {
                throw new IllegalArgumentException("Geçersiz URL protokolü. Sadece HTTP ve HTTPS desteklenir.");
            }
            
            // Potansiyel zararlı URL'leri engelle (basit bir kontrol)
            if (url.getHost().contains("localhost") || url.getHost().equals("127.0.0.1") || url.getHost().equals("0.0.0.0")) {
                throw new IllegalArgumentException("Localhost adresleri kabul edilmez.");
            }
            
            // URL'in şeması, host ve portu olmalı
            if (url.getHost() == null || url.getHost().isEmpty()) {
                throw new IllegalArgumentException("URL'de geçerli bir host yok.");
            }
            
            log.debug("URL validated successfully: {}", urlString);
        } catch (MalformedURLException e) {
            log.error("Invalid URL format: {}", urlString, e);
            throw new IllegalArgumentException("Geçersiz URL formatı");
        }
    }
}
