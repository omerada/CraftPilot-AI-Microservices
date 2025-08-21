# 🐳 CraftPilot AI - Docker Deployment

## 🚀 Quick Docker Setup

### Prerequisites

- **Docker 20.10+**
- **Docker Compose 2.0+**
- **8GB RAM** minimum
- **Firebase service account** file

## 📦 Build & Deploy

### 1. Prepare Environment

```bash
# Clone repository
git clone https://github.com/omerada/CraftPilot-API.git
cd CraftPilot-API

# Place Firebase credentials
cp firebase-service-account.json scripts/
```

### 2. Build All Services

```bash
# Build Maven projects
mvn clean package -DskipTests

# Build Docker images
docker-compose build
```

### 3. Start Infrastructure

```bash
# Start Redis, Prometheus, Grafana
docker-compose -f docker-compose.infra.yml up -d
```

### 4. Start Services

```bash
# Start all microservices
docker-compose up -d

# Check status
docker-compose ps
```

## 🔧 Environment Configuration

### Environment Variables

```bash
# Create .env file
cat > .env << EOF
FIREBASE_PROJECT_ID=your-project-id
REDIS_PASSWORD=your-redis-password
POSTGRES_PASSWORD=your-db-password
JWT_SECRET=your-jwt-secret
SPRING_PROFILES_ACTIVE=prod
EOF
```

### Service Ports

| Service       | Internal Port | External Port |
| ------------- | ------------- | ------------- |
| API Gateway   | 8080          | 8080          |
| Eureka Server | 8761          | 8761          |
| User Service  | 8081          | -             |
| LLM Service   | 8082          | -             |
| Image Service | 8083          | -             |
| Redis         | 6379          | 6379          |
| Prometheus    | 9090          | 9090          |
| Grafana       | 3000          | 3000          |

## 📊 Monitoring Setup

### Prometheus Configuration

```yaml
# prometheus/prometheus.yml
global:
  scrape_interval: 15s

scrape_configs:
  - job_name: "spring-actuator"
    metrics_path: "/actuator/prometheus"
    static_configs:
      - targets:
          - "api-gateway:8080"
          - "user-service:8081"
          - "llm-service:8082"
```

### Grafana Dashboards

- **System Overview**: CPU, Memory, Network
- **Application Metrics**: Request rates, Response times
- **Business Metrics**: User activities, Service usage

## 🔐 Security in Docker

### Secrets Management

```bash
# Create Docker secrets
echo "your-firebase-key" | docker secret create firebase_key -
echo "your-redis-password" | docker secret create redis_password -
```

### Network Security

```yaml
# docker-compose.yml
networks:
  app-network:
    driver: bridge
    internal: true
  web-network:
    driver: bridge
```

## 📝 Docker Compose Files

### Main Services (docker-compose.yml)

```yaml
version: "3.8"

services:
  eureka-server:
    build: ./eureka-server
    ports:
      - "8761:8761"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
    networks:
      - app-network

  api-gateway:
    build: ./api-gateway
    ports:
      - "8080:8080"
    depends_on:
      - eureka-server
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE=http://eureka-server:8761/eureka
    networks:
      - app-network
      - web-network
```

### Infrastructure (docker-compose.infra.yml)

```yaml
version: "3.8"

services:
  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
    command: redis-server --requirepass ${REDIS_PASSWORD}
    networks:
      - app-network

  prometheus:
    image: prom/prometheus:latest
    ports:
      - "9090:9090"
    volumes:
      - ./prometheus:/etc/prometheus
    networks:
      - app-network
```

## 🔄 Health Checks

### Service Health

```bash
# Check all services
docker-compose ps

# Check specific service logs
docker-compose logs -f user-service

# Health endpoints
curl http://localhost:8080/actuator/health
curl http://localhost:8761/actuator/health
```

### Container Health

```yaml
healthcheck:
  test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
  interval: 30s
  timeout: 10s
  retries: 3
  start_period: 40s
```

## 🚀 Production Deployment

### Resource Limits

```yaml
services:
  user-service:
    deploy:
      resources:
        limits:
          cpus: "1.0"
          memory: 1G
        reservations:
          cpus: "0.5"
          memory: 512M
```

### Scaling

```bash
# Scale specific service
docker-compose up -d --scale user-service=3

# Auto-scaling with Docker Swarm
docker service create --replicas 3 --name user-service craftpilot/user-service
```

## 🔧 Troubleshooting

### Common Issues

```bash
# Service connection issues
docker network ls
docker network inspect craftpilot_app-network

# Memory issues
docker stats

# Log analysis
docker-compose logs --tail=100 -f user-service
```

### Debug Mode

```bash
# Run with debug
docker-compose -f docker-compose.yml -f docker-compose.debug.yml up -d

# Connect to container
docker exec -it craftpilot_user-service_1 bash
```

## 📦 Image Management

### Build Strategy

```bash
# Multi-stage build for optimization
FROM maven:3.9.6-eclipse-temurin-21 AS build
FROM eclipse-temurin:21-jre-jammy AS runtime
```

### Image Registry

```bash
# Tag and push
docker tag craftpilot/user-service:latest registry.craftpilot.com/user-service:v1.0.0
docker push registry.craftpilot.com/user-service:v1.0.0
```
