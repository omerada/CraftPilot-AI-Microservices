# Content Service

Content Service, Craft Pilot AI platformunun içerik yönetimi mikroservisidir. Bu servis, içeriklerin oluşturulması, güncellenmesi, silinmesi ve aranması gibi temel işlemleri sağlar.

## Özellikler

- Reaktif programlama ile yüksek performanslı API
- Event-driven mimari ile asenkron iletişim
- Redis önbellekleme ile hızlı veri erişimi
- Google Cloud Firestore ile ölçeklenebilir veri depolama
- Kafka ile güvenilir mesajlaşma
- Circuit breaker ve retry mekanizmaları ile hata toleransı
- Rate limiting ile API koruması
- Prometheus metrikler ile izleme
- Kubernetes ile container orchestration

## Teknolojiler

- Java 21
- Spring Boot 3.2.2
- Spring WebFlux
- Spring Cloud
- Google Cloud Firestore
- Redis
- Apache Kafka
- Resilience4j
- Prometheus
- Docker
- Kubernetes

## Başlangıç

### Gereksinimler

- JDK 17
- Maven
- Docker
- Kubernetes
- Google Cloud hesabı
- Redis
- Kafka

### Kurulum

1. Projeyi klonlayın:

```bash
git clone https://github.com/craftpilot/content-service.git
cd content-service
```

2. Bağımlılıkları yükleyin:

```bash
mvn clean install
```

3. Docker imajını oluşturun:

```bash
docker build -t craftpilot/content-service .
```

4. Kubernetes yapılandırmalarını uygulayın:

```bash
kubectl apply -f k8s/
```

### Yapılandırma

Servis aşağıdaki ortam değişkenlerini kullanır:

- `SPRING_PROFILES_ACTIVE`: Aktif Spring profili
- `SPRING_CLOUD_GCP_PROJECT_ID`: Google Cloud proje ID'si
- `SPRING_CLOUD_GCP_FIRESTORE_DATABASE_ID`: Firestore veritabanı ID'si
- `SPRING_REDIS_HOST`: Redis sunucu adresi
- `SPRING_REDIS_PORT`: Redis port numarası
- `SPRING_KAFKA_BOOTSTRAP_SERVERS`: Kafka sunucu adresleri

### API Endpoints

#### İçerik İşlemleri

- `POST /api/v1/contents`: Yeni içerik oluşturma
- `GET /api/v1/contents/{id}`: İçerik getirme
- `PUT /api/v1/contents/{id}`: İçerik güncelleme
- `DELETE /api/v1/contents/{id}`: İçerik silme
- `PUT /api/v1/contents/{id}/publish`: İçerik yayınlama
- `PUT /api/v1/contents/{id}/archive`: İçerik arşivleme

#### İçerik Arama

- `GET /api/v1/contents/user/{userId}`: Kullanıcı içeriklerini getirme
- `GET /api/v1/contents/type/{type}`: Tipe göre içerik arama
- `GET /api/v1/contents/status/{status}`: Duruma göre içerik arama
- `GET /api/v1/contents/tags`: Etiketlere göre içerik arama
- `GET /api/v1/contents/metadata`: Metadataya göre içerik arama

### Metrikler

Servis Prometheus metrikleri `/actuator/prometheus` endpoint'inden sunar:

- HTTP istek metrikleri
- Cache hit/miss oranları
- Circuit breaker durumları
- JVM metrikleri

### Sağlık Kontrolü

- Readiness probe: `/actuator/health/readiness`
- Liveness probe: `/actuator/health/liveness`

## Test

Unit testleri çalıştırmak için:

```bash
mvn test
```

## CI/CD

Servis GitHub Actions ile CI/CD pipeline'ına sahiptir:

1. Build ve test
2. Güvenlik taraması
3. Docker imaj oluşturma
4. Kubernetes deployment

## Lisans

Bu proje MIT lisansı altında lisanslanmıştır.
