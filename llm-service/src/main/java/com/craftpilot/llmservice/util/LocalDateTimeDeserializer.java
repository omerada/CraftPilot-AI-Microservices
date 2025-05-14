package com.craftpilot.llmservice.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.Instant;
import java.time.ZoneId;
import org.springframework.stereotype.Component;

@Component
public class LocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {

    @Override
    public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        try {
            // JSON içeriğinden long değeri al (unix timestamp olarak)
            long timestamp = p.getValueAsLong();
            if (timestamp <= 0) {
                return LocalDateTime.now();
            }

            // Millisaniye cinsinden timestamp'i LocalDateTime'a dönüştür
            return LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
        } catch (Exception e) {
            // Hata durumunda şu anki zamanı döndür
            return LocalDateTime.now();
        }
    }
}
