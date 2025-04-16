# Lighthouse Analysis Service

Bu servis, web sitesi performans analizlerini Google Lighthouse CLI kullanarak gerçekleştirir ve Redis kuyruk sistemi aracılığıyla yönetir.

## Özellikler

- Spring WebFlux tabanlı reaktif API
- Redis kuyruklama sistemi
- Docker üzerinde çalışabilirlik
- Gelişmiş health check ve metrikler
- Rate limiting (IP bazlı)
- CORS yapılandırması
- Redis bağlantı hatalarına karşı dayanıklılık
- Gerçek Lighthouse CLI entegrasyonu ile profesyonel analiz
- Basit ve detaylı analiz seçenekleri

## Ön Gereksinimler

- Java 17 veya üzeri
- Redis
- Google Chrome/Chromium
- Node.js ve NPM
- Lighthouse CLI (`npm install -g lighthouse`)

Docker kullanıyorsanız, sağlanan Dockerfile tüm gerekli bağımlılıkları içerir.

## API Endpoints

### Performans Analizi İsteği

```
POST /api/v1/analyze
```

**Request Body:**

```json
{
  "url": "https://example.com",
  "analysisType": "basic" // veya "detailed"
}
```

**Response:**

```json
{
  "jobId": "a1b2c3d4-e5f6-g7h8-i9j0",
  "status": "PENDING",
  "url": "https://example.com",
  "analysisType": "basic",
  "queuePosition": 1,
  "estimatedWaitTime": 30
}
```

#### Analiz Tipleri

- **basic**: Hızlı ve temel performans analizi (sadece performans kategorisini analiz eder, yaklaşık 30-90 saniye)
- **detailed**: Kapsamlı ve detaylı analiz (tüm kategorileri analiz eder, gerçekçi throttling kullanır, yaklaşık 1-3 dakika)

Analiz süreleri analiz edilen web sitesinin karmaşıklığına ve sunucu yükü seviyesine göre değişkenlik gösterebilir.

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
  "application": "UP",
  "redis": "UP",
  "timestamp": 1680123456789,
  "details": {
    "liveness": "CORRECT",
    "readiness": "ACCEPTING_TRAFFIC",
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
    },
    "ping": {
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
- `REDIS_HOST`: Redis sunucu adresi (varsayılan: redis)
- `REDIS_PORT`: Redis port numarası (varsayılan: 6379)
- `REDIS_PASSWORD`: Redis şifresi
- `LIGHTHOUSE_CLI_PATH`: Lighthouse CLI yolu (varsayılan: lighthouse)
- `LIGHTHOUSE_TEMP_DIR`: Geçici dosyalar için dizin (varsayılan: /tmp)
- `LIGHTHOUSE_JOB_TIMEOUT`: Analiz işlem zaman aşımı (varsayılan: 180 saniye)
- `LIGHTHOUSE_WORKER_COUNT`: Eşzamanlı worker sayısı (varsayılan: 3)
- `REDIS_CONNECT_TIMEOUT`: Redis bağlantı zaman aşımı (varsayılan: 2000ms)
- `LIGHTHOUSE_QUEUE_NAME`: BullMQ kuyruk adı (varsayılan: lighthouse-jobs)
- `LIGHTHOUSE_RESULTS_PREFIX`: Sonuç anahtarı öneki (varsayılan: lighthouse-results:)
- `LIGHTHOUSE_REDIS_CONNECTION_MAX_ATTEMPTS`: Redis bağlantı deneme sayısı (varsayılan: 5)
- `LIGHTHOUSE_REDIS_CONNECTION_RETRY_DELAY_MS`: Bağlantı denemeleri arası bekleme süresi (varsayılan: 3000ms)
