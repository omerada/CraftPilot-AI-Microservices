# Translation Service

Translation Service, Craft Pilot AI platformunun çeviri mikroservisidir. Bu servis, OpenAI API'sini kullanarak metin çevirisi yapar ve çeviri geçmişini yönetir.

## Özellikler

- Metin çevirisi
- Çeviri geçmişi yönetimi
- Redis önbellekleme
- Firestore veritabanı entegrasyonu
- Kafka event yayını

## Gereksinimler

- Java 21
- Maven
- Redis
- Kafka
- Firebase hesabı ve yapılandırması

## Kurulum

1. Firebase servis hesabı anahtarını indirin ve projenin kök dizinine yerleştirin.
2. `application.yml` dosyasındaki yapılandırmaları güncelleyin:
   - Redis bağlantı bilgileri
   - Kafka bağlantı bilgileri
   - Firebase yapılandırması
   - OpenAI API anahtarı

## Çalıştırma

### Geliştirme Ortamı

```bash
./mvnw spring-boot:run
```

### Docker ile Çalıştırma

```bash
docker build -t craftpilot/translation-service .
docker run -p 1116:1116 craftpilot/translation-service
```

## API Endpoints

### Çeviri Yönetimi

- `POST /api/translations` - Yeni çeviri oluştur
- `GET /api/translations/{id}` - Çeviri getir
- `GET /api/translations/user/{userId}` - Kullanıcının çevirilerini getir
- `DELETE /api/translations/{id}` - Çeviri sil

### Çeviri Geçmişi Yönetimi

- `POST /api/translation-histories` - Yeni çeviri geçmişi kaydet
- `GET /api/translation-histories/{id}` - Çeviri geçmişi getir
- `GET /api/translation-histories/user/{userId}` - Kullanıcının son 5 çeviri geçmişini getir
- `DELETE /api/translation-histories/{id}` - Çeviri geçmişi sil

## Yapılandırma

### application.yml

```yaml
server:
  port: 1116

spring:
  application:
    name: translation-service
  redis:
    host: localhost
    port: 6379
  kafka:
    bootstrap-servers: localhost:9092

firebase:
  credential:
    path: /path/to/firebase-credentials.json
  project:
    id: your-project-id

openai:
  api-key: your-api-key
  model: gpt-4-turbo-preview
  max-tokens: 2000
  temperature: 0.3
```

## Metrikler

Servis, Prometheus ve Actuator endpoint'leri üzerinden metrik sağlar:

- `/actuator/health` - Servis sağlık durumu
- `/actuator/metrics` - JVM ve uygulama metrikleri
- `/actuator/prometheus` - Prometheus formatında metrikler

## Lisans

Bu proje MIT lisansı altında lisanslanmıştır.
