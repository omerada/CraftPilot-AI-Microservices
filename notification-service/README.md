# Notification Service

Bu servis, CraftPilot platformunda bildirim yönetiminden sorumludur. E-posta bildirimleri, push bildirimleri ve diğer bildirim türlerini yönetir.

## Özellikler

- Reaktif programlama ile yüksek performanslı bildirim işleme
- E-posta bildirimleri (SendGrid entegrasyonu)
- Push bildirimleri (Firebase Cloud Messaging entegrasyonu)
- Bildirim şablonları yönetimi
- Kullanıcı bildirim tercihleri yönetimi
- Kafka ile event-driven mimari
- Redis ile önbellekleme
- Circuit breaker, retry ve rate limiting ile dayanıklılık
- Prometheus ve Micrometer ile metrik toplama
- OpenAPI/Swagger ile API dokümantasyonu

## Teknolojiler

- Java 21
- Spring Boot 3.2.3
- Spring WebFlux
- Spring Cloud Stream
- Apache Kafka
- Redis
- Firebase Admin SDK
- SendGrid API
- Resilience4j
- Prometheus
- OpenAPI 3.0

## Başlangıç

### Gereksinimler

- JDK 17
- Maven
- Redis
- Kafka
- Firebase hesabı ve yapılandırması
- SendGrid API anahtarı

### Ortam Değişkenleri

```properties
KAFKA_BROKERS=localhost:9092
REDIS_HOST=localhost
REDIS_PORT=6379
GCP_SA_KEY_PATH=path/to/firebase-credentials.json
SENDGRID_API_KEY=your-sendgrid-api-key
GOOGLE_CLOUD_PROJECT_ID=your-project-id
EUREKA_SERVER_URL=http://localhost:8761/eureka
```

### Derleme

```bash
mvn clean package
```

### Çalıştırma

```bash
java -jar target/notification-service-1.0.0.jar
```

## API Dokümantasyonu

Swagger UI: `http://localhost:8083/swagger-ui.html`

## Metrikler

Prometheus endpoint: `http://localhost:8083/actuator/prometheus`

## Sağlık Kontrolü

Actuator endpoint: `http://localhost:8083/actuator/health`

## Katkıda Bulunma

1. Bu depoyu fork edin
2. Feature branch'inizi oluşturun (`git checkout -b feature/amazing-feature`)
3. Değişikliklerinizi commit edin (`git commit -m 'feat: Add amazing feature'`)
4. Branch'inizi push edin (`git push origin feature/amazing-feature`)
5. Pull Request oluşturun

## Lisans

Bu proje Apache 2.0 lisansı altında lisanslanmıştır. Detaylar için [LICENSE](LICENSE) dosyasına bakın.
