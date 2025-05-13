package com.craftpilot.imageservice.config;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import org.springframework.kafka.core.KafkaAdmin;
import java.time.Duration;

@Configuration
@Slf4j
public class HealthConfig {

    @Bean
    public HealthIndicator mongoHealthIndicator(ReactiveMongoTemplate mongoTemplate) {
        return () -> {
            try {
                // Kısa bir zaman aşımı ile MongoDB'ye ping gönder
                Boolean isAvailable = mongoTemplate.executeCommand("{ ping: 1 }")
                    .map(document -> true) // Komut başarılı olursa true döndür
                    .timeout(Duration.ofSeconds(5))
                    .onErrorResume(e -> {
                        log.warn("MongoDB sağlık kontrolü başarısız: {}", e.getMessage());
                        return Mono.just(false);
                    })
                    .blockOptional(Duration.ofSeconds(6))
                    .orElse(false);
                
                if (isAvailable) {
                    return Health.up().withDetail("database", "MongoDB").build();
                } else {
                    return Health.down()
                        .withDetail("database", "MongoDB")
                        .withDetail("message", "MongoDB bağlantısı kurulamadı")
                        .build();
                }
            } catch (Exception e) {
                log.warn("MongoDB sağlık kontrolü başarısız: {}", e.getMessage());
                return Health.down()
                    .withDetail("database", "MongoDB")
                    .withDetail("error", e.getMessage())
                    .build();
            }
        };
    }

    @Bean
    public HealthIndicator customKafkaHealthIndicator(KafkaAdmin kafkaAdmin) {
        return () -> {
            try {
                // Kafka bootstrap sunucularının yapılandırıldığını kontrol et
                Object bootstrapServers = kafkaAdmin.getConfigurationProperties()
                    .get("bootstrap.servers");
                
                if (bootstrapServers != null && !String.valueOf(bootstrapServers).isEmpty()) {
                    return Health.up()
                        .withDetail("bootstrapServers", bootstrapServers)
                        .build();
                } else {
                    return Health.down()
                        .withDetail("message", "Kafka bootstrap sunucuları yapılandırılmamış")
                        .build();
                }
            } catch (Exception e) {
                log.warn("Kafka sağlık kontrolü başarısız: {}", e.getMessage());
                return Health.down()
                    .withDetail("error", e.getMessage())
                    .build();
            }
        };
    }
}
