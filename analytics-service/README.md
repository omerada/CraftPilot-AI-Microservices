# Analytics Service

Analytics Service, Craft Pilot platformunun analitik ve raporlama işlemlerini gerçekleştiren mikroservistir. Bu servis, platform üzerindeki kullanıcı davranışlarını, sistem metriklerini ve iş analizlerini yönetir.

## Özellikler

- Kullanıcı davranış analizi
- Sistem performans metrikleri
- İş metrikleri ve KPI'lar
- Özelleştirilebilir raporlama
- Real-time analitik

## Teknolojiler

- Java 21
- Spring Boot 3.x
- Spring WebFlux
- Apache Kafka
- Google Cloud Firestore
- Prometheus/Grafana
- OpenAPI/Swagger
- JaCoCo
- SonarQube

## Başlangıç

### Gereksinimler

- JDK 17
- Maven
- Docker
- Kubernetes
- Kafka
- Google Cloud hesabı

### Kurulum

1. Projeyi klonlayın:

```bash
git clone https://github.com/craftpilot/analytics-service.git
```

2. Bağımlılıkları yükleyin:

```bash
mvn clean install
```

3. Uygulamayı çalıştırın:

```bash
mvn spring-boot:run
```

### Docker ile Çalıştırma

```bash
docker build -t craftpilot/analytics-service .
docker run -p 8082:8082 craftpilot/analytics-service
```

### Kubernetes ile Deployment

```bash
kubectl apply -f k8s/
```

## API Dokümantasyonu

Swagger UI: `http://localhost:8082/swagger-ui.html`

## Metrikler ve İzleme

- Prometheus metrikleri: `http://localhost:8082/actuator/prometheus`
- Health check: `http://localhost:8082/actuator/health`

## Performans

Service aşağıdaki performans kriterlerini karşılamaktadır:

- Response time: P95 < 500ms
- Throughput: 100 RPS
- Error rate: < 1%

## Test

```bash
# Unit ve entegrasyon testleri
mvn test

# Güvenlik taraması
mvn dependency-check:check

# Performans testi
k6 run src/test/k6/performance-test.js
```

## CI/CD

GitHub Actions ile otomatik CI/CD pipeline:

- Build ve test
- Güvenlik taraması
- SonarQube kod analizi
- Docker image build
- Kubernetes deployment

## Monitoring

- Prometheus metrik toplama
- Grafana dashboard'ları
- Kafka consumer/producer metrikleri
- Custom business metrikleri

## Lisans

Apache License 2.0
