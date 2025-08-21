# 🔧 CraftPilot AI - Development Guide

## 🛠️ Development Environment

### Prerequisites

- **JDK 21** (Eclipse Temurin recommended)
- **Maven 3.8+**
- **IntelliJ IDEA** or **VS Code**
- **Docker Desktop**
- **Git**

### IDE Setup

```bash
# IntelliJ IDEA Settings
- Java SDK: 21
- Maven: Use bundled
- Code Style: Google Java Format
- Plugins: Spring Boot, Lombok, Docker
```

## 📁 Project Structure

```
CraftPilot-API/
├── api-gateway/           # Gateway service (8080)
├── eureka-server/         # Service discovery (8761)
├── user-service/          # User management (8081)
├── llm-service/           # AI language models (8082)
├── image-service/         # Image generation (8083)
├── subscription-service/  # Plans & billing (8084)
├── credit-service/        # Credit system (8085)
├── notification-service/  # Notifications (8086)
├── analytics-service/     # Analytics (8087)
├── admin-service/         # Admin panel (8088)
├── user-memory-service/   # User context (8089)
├── activity-log-service/  # Audit logs (8090)
├── craft-pilot-commons/   # Shared utilities
├── redis-client-lib/      # Redis client library
└── docs/                  # Documentation
```

## 🔧 Local Development

### 1. Start Infrastructure

```bash
# Redis, Prometheus, Grafana
docker-compose -f docker-compose.infra.yml up -d
```

### 2. Start Core Services

```bash
# Start in order
cd eureka-server && mvn spring-boot:run &
cd api-gateway && mvn spring-boot:run &
cd user-service && mvn spring-boot:run &
```

### 3. Development with Hot Reload

```bash
# Spring Boot DevTools enabled
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Dspring.profiles.active=dev"
```

## 🧪 Testing

### Unit Tests

```bash
mvn test
```

### Integration Tests

```bash
mvn verify
```

### Test Coverage

```bash
mvn jacoco:report
# Report: target/site/jacoco/index.html
```

## 🔍 Code Quality

### Formatting

```bash
mvn spotless:apply
```

### Static Analysis

```bash
mvn spotbugs:check
mvn checkstyle:check
```

## 🐛 Debugging

### Remote Debug

```bash
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005"
```

### Debug Ports

- User Service: 5001
- LLM Service: 5002
- Image Service: 5003
- Gateway: 5000

## 📊 Monitoring in Development

### Health Checks

```bash
curl http://localhost:8080/actuator/health
curl http://localhost:8081/actuator/health
```

### Metrics

```bash
curl http://localhost:8080/actuator/prometheus
```

### Service Discovery

- Eureka: http://localhost:8761

## 🔄 Git Workflow

### Branch Naming

- `feature/feature-name`
- `bugfix/bug-description`
- `hotfix/critical-fix`

### Commit Convention

```bash
feat: add new user authentication
fix: resolve memory leak in LLM service
docs: update API documentation
```

## 📦 Build & Package

### Local Build

```bash
mvn clean package -DskipTests
```

### Docker Build

```bash
# Build all services
./scripts/build-all.sh

# Build single service
docker build -t craftpilot/user-service user-service/
```

## 🚀 Environment Profiles

### Development (dev)

- Hot reload enabled
- Debug logging
- In-memory cache

### Production (prod)

- Optimized JVM settings
- Error logging only
- Distributed cache

### Configuration

```yaml
spring:
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:dev}
```
