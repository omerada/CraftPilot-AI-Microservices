package com.craftpilot.llmservice.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.cloud.Timestamp;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.concurrent.TimeUnit;

/**
 * Frontend'den gelen çeşitli timestamp formatlarını Google Cloud Timestamp'e çeviren deserializer
 */
@Slf4j
public class TimestampDeserializer extends JsonDeserializer<Timestamp> {

    @Override
    public Timestamp deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        try {
            if (p.getCurrentToken().isNumeric()) {
                // Sayısal değer (unix timestamp) - saniye veya milisaniye olabilir
                long value = p.getLongValue();
                
                // 10 karakterli ise saniye, 13 karakterli ise milisaniye olarak kabul et
                if (String.valueOf(value).length() <= 10) {
                    // Saniye cinsinden
                    return Timestamp.ofTimeSecondsAndNanos(value, 0);
                } else {
                    // Milisaniye cinsinden
                    return Timestamp.ofTimeSecondsAndNanos(
                        TimeUnit.MILLISECONDS.toSeconds(value),
                        (int) TimeUnit.MILLISECONDS.toNanos(value % 1000)
                    );
                }
            } else if (p.getCurrentToken().isScalarValue()) {
                // String değer - ISO-8601 formatı veya başka bir tarih formatı olabilir
                String text = p.getValueAsString();
                
                try {
                    // ISO-8601 formatını parse et
                    Instant instant = Instant.parse(text);
                    return Timestamp.ofTimeSecondsAndNanos(
                        instant.getEpochSecond(),
                        instant.getNano()
                    );
                } catch (DateTimeParseException e) {
                    // Alternatif format denemeleri
                    log.warn("ISO-8601 tarih formatı parse edilemedi: {}", text);
                    
                    // Şu anki zamanı kullan
                    return Timestamp.now();
                }
            } else if (p.getCurrentToken().isStructStart()) {
                // JSON nesnesi - Firebase Timestamp formatı olabilir
                p.skipChildren();
                
                // Geçerli bir format bulunamadı, varsayılan değeri döndür
                log.warn("Geçerli bir timestamp formatı değil (JSON nesnesi)");
                return Timestamp.now();
            }
            
            // Hiçbir format uygun değilse varsayılan zamanı döndür
            log.warn("Bilinmeyen timestamp formatı, şu anki zaman kullanılıyor");
            return Timestamp.now();
        } catch (Exception e) {
            log.error("Timestamp dönüşümü sırasında hata: {}", e.getMessage());
            return Timestamp.now();
        }
    }
}
