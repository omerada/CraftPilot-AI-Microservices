# 🚀 CraftPilot AI - Saas Backend Platform

<div align="center">

```
                    ┌─────────────────────────────────────────┐
                    │            🌐 Frontend Layer            │
                    │        Web UI  •  Mobile  •  API       │
                    └─────────────────┬───────────────────────┘
                                      │
                    ┌─────────────────┴───────────────────────┐
                    │         🚪 API Gateway (8080)          │
                    │     Authentication  •  Rate Limiting   │
                    │        Routing  •  Load Balancing      │
                    └─────┬─────────┬─────────┬───────────────┘
                          │         │         │
           ┌──────────────┴───┐ ┌───┴───┐ ┌───┴─────────────────────┐
           │                  │ │       │ │                         │
    ┌──────▼──────┐    ┌──────▼──────┐ ┌▼──────┐    ┌──────────────▼──────┐
    │ 👤 User      │    │ 🤖 LLM      │ │🖼️ Image│    │ 💳 Subscription     │
    │ Service      │    │ Service     │ │Service │    │ Service             │
    │ (8081)       │    │ (8082)      │ │(8083)  │    │ (8084)              │
    └──────┬──────┘    └──────┬──────┘ └┬──────┘    └──────────────┬──────┘
           │                  │         │                          │
    ┌──────▼──────┐    ┌──────▼──────┐ ┌▼──────┐    ┌──────────────▼──────┐
    │ 💰 Credit    │    │ 🔔 Notification│📊 Analytics│ 🛡️ Admin            │
    │ Service      │    │ Service     │ │Service │    │ Service             │
    │ (8085)       │    │ (8086)      │ │(8087)  │    │ (8088)              │
    └──────┬──────┘    └──────┬──────┘ └┬──────┘    └──────────────┬──────┘
           │                  │         │                          │
           └──────────────────┼─────────┼──────────────────────────┘
                              │         │
              ┌───────────────▼─────────▼───────────────┐
              │        📡 Service Discovery             │
              │         Eureka Server (8761)           │
              └───────────────┬─────────────────────────┘
                              │
    ┌─────────────────────────┼─────────────────────────────────────┐
    │                         │                                     │
┌───▼────┐  ┌──────▼──────┐  ┌▼──────┐  ┌─────────▼─────────┐  ┌───▼────┐
│🗄️ Redis │  │📊 Prometheus │  │Kafka  │  │  🔥 Firebase       │  │📈Grafana│
│ Cache   │  │  Metrics    │  │Message│  │  Firestore        │  │Dashboard│
│         │  │             │  │Broker │  │  Authentication   │  │        │
└─────────┘  └─────────────┘  └───────┘  └───────────────────┘  └────────┘

           🚀 Reactive • ⚡ Real-time • 🔒 Secure • 📈 Scalable
```

</div>

![Java](https://img.shields.io/badge/Java-21-orange?style=for-the-badge&logo=java)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.1-green?style=for-the-badge&logo=spring)
![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-2023.0.2-blue?style=for-the-badge&logo=spring)
![Firebase](https://img.shields.io/badge/Firebase-Admin-orange?style=for-the-badge&logo=firebase)
![Docker](https://img.shields.io/badge/Docker-Ready-blue?style=for-the-badge&logo=docker)
![Kubernetes](https://img.shields.io/badge/Kubernetes-Compatible-purple?style=for-the-badge&logo=kubernetes)

**Gelişmiş AI destekli SaaS platformu - Microservices mimarisi ile yapılandırılmış**

[🔗 Demo](https://craftpilot.io) • [📖 Dokümantasyon](./docs) • [🐛 Issue Bildirimi](../../issues) • [💬 Tartışmalar](../../discussions)

</div>

---

## 📋 İçindekiler

- [Genel Bakış](#-genel-bakış)
- [🔐 Güvenlik](#-güvenlik)
- [Özellikler](#-özellikler)
- [Mimari](#-mimari)
- [Servisler](#-servisler)
- [Teknoloji Yığını](#-teknoloji-yığını)
- [Kurulum](#-kurulum)
- [Kullanım](#-kullanım)
- [API Dokümantasyonu](#-api-dokümantasyonu)
- [Monitoring & Observability](#-monitoring--observability)
- [Katkıda Bulunma](#-katkıda-bulunma)
- [Lisans](#-lisans)

---

> **🔐 SECURITY NOTICE**: This repository contains template configurations with placeholder passwords. **Before deployment, you MUST change all default passwords and configure proper secret management.** See [SECURITY.md](./SECURITY.md) for detailed security setup instructions.

---

## 🌟 Genel Bakış

**CraftPilot AI**, modern işletmeler için tasarlanmış, kapsamlı yapay zeka destekli SaaS platformudur. Mikroservis mimarisi kullanarak ölçeklenebilir, güvenilir ve yüksek performanslı AI çözümleri sunar.

### 🎯 Temel Amaç

- **AI Entegrasyonu**: GPT, Claude gibi LLM modelleriyle güçlü konuşma deneyimi
- **Görsel AI**: Gelişmiş image generation ve processing yetenekleri
- **Enterprise Ready**: Kurumsal düzeyde güvenlik, monitoring ve ölçeklenebilirlik
- **Real-time**: Reactive programming ile gerçek zamanlı veri işleme
- **Cloud Native**: Kubernetes ve Docker ile cloud-first yaklaşım

---

## 🔐 Güvenlik

> **⚠️ UYARI**: Bu proje şu anda development template konfigürasyonları içermektedir.

### 🚨 Production Öncesi Gereksinimler

**Bu projeyi production'da kullanmadan önce mutlaka:**

1. **📋 [SECURITY.md](./SECURITY.md) dosyasını okuyun** - Detaylı güvenlik kurulum talimatları
2. **� [DEPLOYMENT_SECURITY.md](./DEPLOYMENT_SECURITY.md) dosyasını inceleyin** - Production deployment güvenlik kılavuzu
3. **�🔑 Tüm placeholder password'ları değiştirin** (`CHANGE_ME_IN_PRODUCTION`)
4. **🔥 Firebase service account'u yapılandırın**
5. **🗝️ GitHub Secrets'ı ayarlayın** (CI/CD için)
6. **🛡️ SSL/TLS sertifikalarını yapılandırın**

### 🔐 Güvenlik Özellikleri

- **Firebase Authentication** - JWT token tabanlı kimlik doğrulama
- **Redis AUTH** - Şifreli cache erişimi
- **Role-based Access Control** - Yetki tabanlı erişim
- **API Rate Limiting** - DDoS koruması
- **Circuit Breaker Pattern** - Hata yalıtımı
- **Distributed Tracing** - Güvenlik monitoring

### 📋 Güvenlik Checklist

- [ ] `.env.example`'dan `.env` oluşturun ve şifreleri değiştirin
- [ ] Firebase credentials'ları yapılandırın
- [ ] GitHub repository secrets'ları ayarlayın
- [ ] Network security rules'ları uygulayın
- [ ] SSL certificates'ları yapılandırın
- [ ] Security scanning yapın

**⚠️ Default şifrelerle production'a çıkmayın!**

---

## ✨ Özellikler

### 🤖 AI & Machine Learning

- **Large Language Models (LLM)** entegrasyonu
- **Image Generation** ve processing servisleri
- **User Memory Service** ile kişiselleştirilmiş deneyim
- **Analytics Service** ile AI kullanım analitikleri

### 🏢 Enterprise Features

- **Multi-tenant** architecture
- **Role-based** access control (RBAC)
- **Subscription Management** sistemi
- **Credit-based** usage tracking
- **Real-time notifications**

### 🔧 Platform Features

- **Reactive Programming** (Spring WebFlux)
- **Event-driven** architecture (Kafka)
- **Circuit Breaker** pattern (Resilience4j)
- **API Gateway** ile centralized routing
- **Service Discovery** (Eureka)
- **Distributed tracing** ve monitoring

---

## 🏗️ Mimari

```mermaid
graph TB
    subgraph "External Layer"
        UI[Web/Mobile UI]
        API_CLIENTS[API Clients]
    end

    subgraph "Gateway Layer"
        GATEWAY[API Gateway<br/>Port: 8080]
    end

    subgraph "Discovery Layer"
        EUREKA[Eureka Server<br/>Port: 8761]
    end

    subgraph "Core Services"
        USER[User Service<br/>Port: 8081]
        LLM[LLM Service<br/>Port: 8082]
        IMAGE[Image Service<br/>Port: 8083]
        SUB[Subscription Service<br/>Port: 8084]
        CREDIT[Credit Service<br/>Port: 8085]
    end

    subgraph "Support Services"
        NOTIFICATION[Notification Service<br/>Port: 8086]
        ANALYTICS[Analytics Service<br/>Port: 8087]
        ADMIN[Admin Service<br/>Port: 8088]
        MEMORY[User Memory Service<br/>Port: 8089]
        ACTIVITY[Activity Log Service<br/>Port: 8090]
    end

    subgraph "Infrastructure"
        REDIS[(Redis Cache)]
        FIREBASE[(Firebase/Firestore)]
        KAFKA[Kafka Message Broker]
        PROMETHEUS[Prometheus]
        GRAFANA[Grafana]
    end

    UI --> GATEWAY
    API_CLIENTS --> GATEWAY
    GATEWAY --> USER
    GATEWAY --> LLM
    GATEWAY --> IMAGE
    GATEWAY --> SUB

    USER --> FIREBASE
    USER --> REDIS
    LLM --> REDIS
    IMAGE --> REDIS

    USER -.-> KAFKA
    LLM -.-> KAFKA
    ACTIVITY -.-> KAFKA

    PROMETHEUS --> GRAFANA
```

### 🔄 Communication Patterns

- **Synchronous**: REST APIs (Spring WebFlux)
- **Asynchronous**: Event-driven messaging (Kafka)
- **Service Discovery**: Eureka Server
- **Load Balancing**: Spring Cloud LoadBalancer
- **Circuit Breaker**: Resilience4j

---

## 🚀 Servisler

### 🛡️ Core Infrastructure

| Servis                 | Port | Açıklama                                   | Teknolojiler                        |
| ---------------------- | ---- | ------------------------------------------ | ----------------------------------- |
| **API Gateway**        | 8080 | Ana giriş noktası, routing, authentication | Spring Cloud Gateway, Firebase Auth |
| **Eureka Server**      | 8761 | Service discovery ve registry              | Netflix Eureka                      |
| **CraftPilot Commons** | -    | Ortak kütüphaneler ve utilities            | Activity logging, Configuration     |

### 👤 Business Services

| Servis                   | Port | Açıklama                        | Özellikler                                              |
| ------------------------ | ---- | ------------------------------- | ------------------------------------------------------- |
| **User Service**         | 8081 | Kullanıcı yönetimi ve tercihler | Profile management, Preferences, Circuit breaker        |
| **LLM Service**          | 8082 | Language model entegrasyonu     | OpenRouter API, Chat histories, Conversation management |
| **Image Service**        | 8083 | AI destekli görsel üretimi      | Image generation, Processing, Storage                   |
| **Subscription Service** | 8084 | Abonelik ve plan yönetimi       | Plan management, Billing, Usage tracking                |
| **Credit Service**       | 8085 | Kredi sistemi yönetimi          | Credit allocation, Usage monitoring, Limits             |

### 📊 Support Services

| Servis                   | Port | Açıklama              | Özellikler                                      |
| ------------------------ | ---- | --------------------- | ----------------------------------------------- |
| **Notification Service** | 8086 | Bildirim sistemi      | Push notifications, Email, SMS                  |
| **Analytics Service**    | 8087 | Platform analitikleri | Usage analytics, Reporting, Insights            |
| **Admin Service**        | 8088 | Yönetim paneli        | User management, System monitoring, Admin tools |
| **User Memory Service**  | 8089 | Kullanıcı hafızası    | Context storage, Personalization                |
| **Activity Log Service** | 8090 | Aktivite takibi       | Audit logs, User activities, Compliance         |

### 🌊 Monitoring & DevOps

| Servis                 | Port | Açıklama                                |
| ---------------------- | ---- | --------------------------------------- |
| **Lighthouse Service** | 8091 | Health monitoring ve lighthouse metrics |
| **Lighthouse Worker**  | -    | Background tasks ve scheduled jobs      |

---

## 🛠️ Teknoloji Yığını

### ☕ Backend Framework

- **Java 21** - Modern Java features
- **Spring Boot 3.3.1** - Enterprise application framework
- **Spring Cloud 2023.0.2** - Microservices toolkit
- **Spring WebFlux** - Reactive programming
- **Maven** - Dependency management

### 🗄️ Database & Storage

- **Firebase/Firestore** - NoSQL document database
- **Redis** - In-memory caching ve session storage
- **Google Cloud Storage** - File storage

### 📨 Messaging & Events

- **Apache Kafka** - Event streaming platform
- **Spring Kafka** - Kafka integration

### 🔐 Security & Authentication

- **Firebase Authentication** - User authentication
- **JWT Tokens** - Stateless authentication
- **Spring Security** - Security framework

### 📊 Monitoring & Observability

- **Prometheus** - Metrics collection
- **Grafana** - Monitoring dashboards
- **Micrometer** - Application metrics
- **Distributed Tracing** - Request tracing

### 🐳 DevOps & Deployment

- **Docker** - Containerization
- **Kubernetes** - Container orchestration
- **GitHub Actions** - CI/CD pipeline
- **Spring Boot Actuator** - Health checks

### 🔧 Resilience & Performance

- **Resilience4j** - Circuit breaker, retry, rate limiting
- **Spring Cloud LoadBalancer** - Client-side load balancing
- **Redis** - Distributed caching

---

## 🚀 Kurulum

### 📋 Gereksinimler

- **Java 21+**
- **Maven 3.8+**
- **Docker & Docker Compose**
- **Redis** (local veya container)
- **Firebase Project** (authentication için)

### 1️⃣ Proje Klonlama

```bash
git clone https://github.com/omerada/CraftPilot-API.git
cd CraftPilot-API
```

### 2️⃣ Environment Setup

```bash
# Firebase configuration dosyasını yerleştirin
cp firebase-service-account.json scripts/

# Environment variables
export FIREBASE_PROJECT_ID=your-project-id
export REDIS_HOST=localhost
export REDIS_PORT=6379
```

### 3️⃣ Infrastructure Services

```bash
# Redis ve monitoring stack'i başlatın
docker-compose -f docker-compose.infra.yml up -d

# Durumu kontrol edin
docker-compose -f docker-compose.infra.yml ps
```

### 4️⃣ Application Build

```bash
# Tüm modülleri build edin
mvn clean install

# Veya sadece package
mvn clean package -DskipTests
```

### 5️⃣ Service Startup (Development)

```bash
# 1. Eureka Server
cd eureka-server && mvn spring-boot:run

# 2. API Gateway
cd api-gateway && mvn spring-boot:run

# 3. Core Services (paralel olarak)
cd user-service && mvn spring-boot:run &
cd llm-service && mvn spring-boot:run &
cd image-service && mvn spring-boot:run &
```

### 🐳 Docker ile Production

```bash
# Tüm servisleri build edin
mvn clean package -DskipTests

# Docker images oluşturun
docker build -t craftpilot/eureka-server eureka-server/
docker build -t craftpilot/api-gateway api-gateway/
docker build -t craftpilot/user-service user-service/
# ... diğer servisler

# Docker Compose ile çalıştırın
docker-compose up -d
```

---

## 💻 Kullanım

### 🌐 Erişim Noktaları

| Servis               | URL                                   | Açıklama             |
| -------------------- | ------------------------------------- | -------------------- |
| **API Gateway**      | http://localhost:8080                 | Ana API endpoint     |
| **Eureka Dashboard** | http://localhost:8761                 | Service discovery UI |
| **Swagger UI**       | http://localhost:8080/swagger-ui.html | API documentation    |
| **Prometheus**       | http://localhost:9090                 | Metrics              |
| **Grafana**          | http://localhost:3000                 | Monitoring dashboard |

### 🔑 Authentication

```bash
# Firebase Authentication token ile API çağrısı
curl -H "Authorization: Bearer YOUR_FIREBASE_TOKEN" \
     http://localhost:8080/api/users/profile
```

### 💬 LLM Service Kullanımı

```bash
# Yeni konuşma başlatma
curl -X POST http://localhost:8080/api/llm/conversations \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"title": "My Conversation"}'

# Mesaj gönderme
curl -X POST http://localhost:8080/api/llm/conversations/{id}/messages \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"content": "Hello, AI!"}'
```

### 🖼️ Image Service Kullanımı

```bash
# Image generation
curl -X POST http://localhost:8080/api/images/generate \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"prompt": "A beautiful sunset", "size": "1024x1024"}'
```

---

## 📚 API Dokümantasyonu

### 🔍 Swagger UI

Tüm servisler için interaktif API dokümantasyonu:

- **Ana API**: http://localhost:8080/swagger-ui.html
- **User Service**: http://localhost:8081/swagger-ui.html
- **LLM Service**: http://localhost:8082/swagger-ui.html
- **Image Service**: http://localhost:8083/swagger-ui.html

### 📖 OpenAPI Specifications

```bash
# OpenAPI JSON specifications
curl http://localhost:8080/v3/api-docs
curl http://localhost:8081/v3/api-docs
curl http://localhost:8082/v3/api-docs
```

### 🔐 Authentication

Tüm API endpoints Firebase JWT token gerektirir:

```
Authorization: Bearer eyJhbGciOiJSUzI1NiIsImtpZCI6...
```

---

## 📊 Monitoring & Observability

### 📈 Metrics

**Prometheus Endpoints**:

- Application metrics: `/actuator/prometheus`
- Custom business metrics: Spring Micrometer
- JVM metrics: Memory, GC, threads

**Key Metrics**:

```
# Request rates
http_server_requests_total
http_server_requests_duration_seconds

# Business metrics
craftpilot_user_registrations_total
craftpilot_llm_requests_total
craftpilot_image_generations_total
craftpilot_credit_usage_total
```

### 📊 Dashboards

**Grafana Dashboards**:

- **Application Overview**: System health, request rates
- **Business Metrics**: User activities, service usage
- **Infrastructure**: JVM metrics, Redis performance
- **Alerts**: Error rates, response times

### 🔍 Distributed Tracing

**Spring Cloud Sleuth** ile request tracing:

```
# Trace headers
X-Trace-Id: 1234567890abcdef
X-Span-Id: abcdef1234567890
```

### 🚨 Health Checks

```bash
# Service health
curl http://localhost:8080/actuator/health
curl http://localhost:8081/actuator/health

# Detailed health info
curl http://localhost:8080/actuator/health/details
```

---

## 🏃‍♂️ Development

### 🛠️ Local Development Setup

```bash
# Hot reload ile development
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Dspring.profiles.active=dev"

# Debug mode
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005"
```

### 🧪 Testing

```bash
# Unit tests
mvn test

# Integration tests
mvn verify

# Test coverage
mvn jacoco:report
```

### 🔄 Code Quality

```bash
# Code formatting
mvn spotless:apply

# Static analysis
mvn spotbugs:check
mvn checkstyle:check
```

---

## 🤝 Katkıda Bulunma

### 📝 Contribution Guidelines

1. **Fork** projeyi
2. **Feature branch** oluşturun (`git checkout -b feature/amazing-feature`)
3. **Commit** yapın (`git commit -m 'Add amazing feature'`)
4. **Push** yapın (`git push origin feature/amazing-feature`)
5. **Pull Request** oluşturun

### 🐛 Bug Reports

[Issue templates](../../issues/new/choose) kullanarak bug raporlayın.

### 💡 Feature Requests

[Discussions](../../discussions) kısmından önerilerde bulunun.

### 📋 Development Standards

- **Java Code Style**: Google Java Style Guide
- **Commit Convention**: Conventional Commits
- **Testing**: Minimum %80 test coverage
- **Documentation**: JavaDoc ve API documentation

---

## 📄 Lisans

Bu proje **MIT License** altında lisanslanmıştır. Detaylar için [LICENSE](LICENSE) dosyasına bakın.

---

## 🙏 Teşekkürler

- **Spring Team** - Framework ve ecosystem
- **Firebase Team** - Authentication ve database services
- **OpenRouter** - LLM API provider
- **Contributors** - Open source community

---

## 📞 İletişim

- **Website**: [craftpilot.io](https://craftpilot.io)
- **Email**: support@craftpilot.com
- **GitHub**: [CraftPilot Organization](https://github.com/craftpilot)
- **Discord**: [Community Server](https://discord.gg/craftpilot)

---

<div align="center">

**⭐ Projeyi beğendiyseniz star vermeyi unutmayın!**

[🔝 Başa Dön](#-craftpilot-ai---enterprise-ai-platform)

</div>
