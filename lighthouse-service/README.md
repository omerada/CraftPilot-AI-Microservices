# Lighthouse Analysis Service

Bu servis, web sitesi performans analizlerini BullMQ ile uyumlu bir Redis kuyruk sistemi üzerinden yönetir.

## Özellikler

- Spring WebFlux tabanlı reaktif API
- BullMQ ile uyumlu Redis kuyruklama sistemi
- Docker üzerinde çalışabilirlik
- Health check ve metrikler
- Rate limiting (IP bazlı)
- CORS yapılandırması

## API Endpoints

### Performans Analizi İsteği

```
POST /api/v1/analyze
```

**Request Body:**

```json
{
  "url": "https://example.com"
}
```

**Response:**

```json
{
  "jobId": "a1b2c3d4-e5f6-g7h8-i9j0"
}
```

### Analiz Sonuçları Sorgusu

```
GET /api/v1/report/{jobId}
```

**Response (işlem devam ediyor):**

```json
{
  "jobId": "a1b2c3d4-e5f6-g7h8-i9j0",
  "status": "waiting",
  "complete": false,
  "createdAt": 1680123456
}
```

**Response (işlem tamamlandı):**

```json
{
  "id": "a1b2c3d4-e5f6-g7h8-i9j0",
  "performance": 0.82,
  "audits": {
    "first-contentful-paint": {
      "score": 0.9,
      "displayValue": "0.8 s",
      "description": "First Contentful Paint"
    },
    ...
  },
  "timestamp": 1680123456789,
  "url": "https://example.com",
  "categories": {
    "performance": {
      "score": 0.82
    },
    ...
  }
}
```

### Health Check

```
GET /health
```

**Response:**

```json
{
  "status": "UP",
  "redis": "UP",
  "timestamp": 1680123456789,
  "details": {
    "connection": "successful"
  }
}
```

Servisin sağlıklı çalışıp çalışmadığını kontrol etmek için kullanılan endpoint. "status" değeri "UP" ise servis çalışıyor demektir.

### Spring Boot Actuator Health

```
GET /actuator/health
```

**Response:**

```json
{
  "status": "UP",
  "components": {
    "diskSpace": {
      "status": "UP"
    },
    "redis": {
      "status": "UP"
    }
  }
}
```

## Redis Anahtarları

- `{bull:lighthouse-jobs}` - BullMQ kuyruk anahtarı
- `{bull:lighthouse-jobs}:id` - Job ID sayacı
- `{bull:lighthouse-jobs}:{job_number}` - Job verisi
- `{bull:lighthouse-jobs}:wait` - Bekleme kuyruğu (zset)
- `lighthouse-results:{job_id}` - Job sonucu

## Kurulum ve Çalıştırma

### Docker Compose ile

```bash
docker-compose up -d
```

### Docker Olmadan

1. Redis sunucusunu başlatın
2. Uygulamayı derleyin: `./mvnw clean package`
3. Uygulamayı çalıştırın: `java -jar target/lighthouse-service-*.jar`

## Ortam Değişkenleri

- `PORT`: Uygulama port numarası (varsayılan: 8085)
- `REDIS_HOST`: Redis sunucu adresi (varsayılan: localhost)
- `REDIS_PORT`: Redis port numarası (varsayılan: 6379)
- `LIGHTHOUSE_QUEUE_NAME`: BullMQ kuyruk adı (varsayılan: lighthouse-jobs)
- `LIGHTHOUSE_RESULTS_PREFIX`: Sonuç anahtarı öneki (varsayılan: lighthouse-results:)
