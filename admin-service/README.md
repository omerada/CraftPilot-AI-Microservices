# Admin Service

Admin Service, Craft Pilot platformunun yönetim işlemlerini gerçekleştiren mikroservistir. Bu servis, platform yöneticilerinin sistem üzerindeki temel operasyonlarını yönetir.

## Özellikler

- Yönetici hesap yönetimi
- Sistem konfigürasyon yönetimi
- Kullanıcı yetkilendirme ve rol yönetimi
- Sistem metrikleri ve izleme

## Teknolojiler

- Java 21
- Spring Boot 3.x
- Spring WebFlux
- Spring Security with OAuth2/JWT
- Redis
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
- Redis
- Firebase yapılandırması

### Kurulum

1. Projeyi klonlayın:

```bash
git clone https://github.com/craftpilot/admin-service.git
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
docker build -t craftpilot/admin-service .
docker run -p 8081:8081 craftpilot/admin-service
```

### Kubernetes ile Deployment

```bash
kubectl apply -f k8s/
```

## API Dokümantasyonu

Swagger UI: `http://localhost:8081/swagger-ui.html`

## Metrikler ve İzleme

- Prometheus metrikleri: `http://localhost:8081/actuator/prometheus`
- Health check: `http://localhost:8081/actuator/health`

## Güvenlik

- JWT tabanlı kimlik doğrulama
- Rate limiting
- SSL/TLS şifreleme
- OWASP güvenlik kontrolleri

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

## Lisans

Apache License 2.0
