# 🚀 CraftPilot AI - Quick Start Guide

## 📋 Prerequisites

- **Java 21+**
- **Maven 3.8+**
- **Docker & Docker Compose**
- **Redis** (local/container)
- **Firebase Account** (for authentication)

## ⚡ Quick Setup

### 1. Clone the project

```bash
git clone https://github.com/omerada/CraftPilot-API.git
cd CraftPilot-API
```

### 2. Firebase configuration

```bash
# Place Firebase service account file
cp firebase-service-account.json scripts/
```

### 3. Environment variables

```bash
export FIREBASE_PROJECT_ID=your-project-id
export REDIS_HOST=localhost
export REDIS_PORT=6379
```

### 4. Start infrastructure

```bash
docker-compose -f docker-compose.infra.yml up -d
```

### 5. Start services

```bash
# Eureka Server
cd eureka-server && mvn spring-boot:run &

# API Gateway
cd api-gateway && mvn spring-boot:run &

# Core Services
cd user-service && mvn spring-boot:run &
cd llm-service && mvn spring-boot:run &
```

## 🌐 Access Points

| Service          | URL                                   | Description       |
| ---------------- | ------------------------------------- | ----------------- |
| API Gateway      | http://localhost:8080                 | Main API          |
| Eureka Dashboard | http://localhost:8761                 | Service discovery |
| Swagger UI       | http://localhost:8080/swagger-ui.html | API docs          |
| Prometheus       | http://localhost:9090                 | Metrics           |
| Grafana          | http://localhost:3000                 | Dashboard         |

## 🔑 Test API Call

```bash
# Health check
curl http://localhost:8080/actuator/health

# Authenticated endpoint
curl -H "Authorization: Bearer YOUR_FIREBASE_TOKEN" \
     http://localhost:8080/api/users/profile
```

## 🐳 Run with Docker

```bash
# Build
mvn clean package -DskipTests

# Run
docker-compose up -d
```
