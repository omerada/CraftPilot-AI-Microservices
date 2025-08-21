# 🚀 CraftPilot AI - Hızlı Başlangıç

## 📋 Gereksinimler

- **Java 21+**
- **Maven 3.8+**
- **Docker & Docker Compose**
- **Redis** (local/container)
- **Firebase Account** (authentication için)

## ⚡ Hızlı Kurulum

### 1. Projeyi klonlayın

```bash
git clone https://github.com/omerada/CraftPilot-API.git
cd CraftPilot-API
```

### 2. Firebase yapılandırması

```bash
# Firebase service account dosyasını yerleştirin
cp firebase-service-account.json scripts/
```

### 3. Environment variables

```bash
export FIREBASE_PROJECT_ID=your-project-id
export REDIS_HOST=localhost
export REDIS_PORT=6379
```

### 4. Infrastructure başlatın

```bash
docker-compose -f docker-compose.infra.yml up -d
```

### 5. Servisleri başlatın

```bash
# Eureka Server
cd eureka-server && mvn spring-boot:run &

# API Gateway
cd api-gateway && mvn spring-boot:run &

# Core Services
cd user-service && mvn spring-boot:run &
cd llm-service && mvn spring-boot:run &
```

## 🌐 Erişim Noktaları

| Servis           | URL                                   | Açıklama          |
| ---------------- | ------------------------------------- | ----------------- |
| API Gateway      | http://localhost:8080                 | Ana API           |
| Eureka Dashboard | http://localhost:8761                 | Service discovery |
| Swagger UI       | http://localhost:8080/swagger-ui.html | API docs          |
| Prometheus       | http://localhost:9090                 | Metrics           |
| Grafana          | http://localhost:3000                 | Dashboard         |

## 🔑 Test API Çağrısı

```bash
# Health check
curl http://localhost:8080/actuator/health

# Authentication gerekli endpoint
curl -H "Authorization: Bearer YOUR_FIREBASE_TOKEN" \
     http://localhost:8080/api/users/profile
```

## 🐳 Docker ile Çalıştırma

```bash
# Build
mvn clean package -DskipTests

# Run
docker-compose up -d
```
