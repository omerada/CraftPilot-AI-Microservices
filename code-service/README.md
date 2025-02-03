# Code Service

Code Service, Craft Pilot AI platformunun kod üretim ve düzenleme mikroservisidir. Bu servis, OpenAI API'sini kullanarak çeşitli programlama dillerinde ve çerçevelerde kod üretir ve yönetir.

## Özellikler

- Kod üretimi
- Kod düzenleme
- Kod geçmişi yönetimi
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
docker build -t craftpilot/code-service .
docker run -p 1117:1117 craftpilot/code-service
```

## API Endpoints

### Kod Yönetimi

- `POST /api/codes` - Yeni kod oluştur
- `GET /api/codes/{id}` - Kod getir
- `GET /api/codes/user/{userId}` - Kullanıcının kodlarını getir
- `DELETE /api/codes/{id}` - Kod sil

### Kod Geçmişi Yönetimi

- `POST /api/code-histories` - Yeni kod geçmişi kaydet
- `GET /api/code-histories/{id}` - Kod geçmişi getir
- `GET /api/code-histories/user/{userId}` - Kullanıcının son 5 kod geçmişini getir
- `DELETE /api/code-histories/{id}` - Kod geçmişi sil

## Yapılandırma

### application.yml

```yaml
server:
  port: 1117

spring:
  application:
    name: code-service
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
  max-tokens: 4000
  temperature: 0.2
```

## Metrikler

Servis, Prometheus ve Actuator endpoint'leri üzerinden metrik sağlar:

- `/actuator/health` - Servis sağlık durumu
- `/actuator/metrics` - JVM ve uygulama metrikleri
- `/actuator/prometheus` - Prometheus formatında metrikler

## Lisans

Bu proje MIT lisansı altında lisanslanmıştır.
