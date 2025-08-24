# 🚀 CraftPilot AI - SaaS Backend Platform

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

**Advanced AI-powered SaaS platform — structured as a microservices architecture**

[🔗 Demo](https://craftpilot.io) • [📖 Documentation](./docs) • [🐛 Report an Issue](../../issues) • [💬 Discussions](../../discussions)

</div>

---

## 📋 Table of Contents

- [Overview](#-overview)
- [🔐 Security](#-security)
- [Features](#-features)
- [Architecture](#-architecture)
- [Services](#-services)
- [Tech Stack](#-tech-stack)
- [Setup](#-setup)
- [Usage](#-usage)
- [API Documentation](#-api-documentation)
- [Monitoring & Observability](#-monitoring--observability)
- [Contributing](#-contributing)
- [License](#-license)

---

> **🔐 SECURITY NOTICE**: This repository contains template configurations with placeholder passwords. **Before deployment, you MUST change all default passwords and configure proper secret management.** See [SECURITY.md](./SECURITY.md) for detailed security setup instructions.

---

## 🌟 Overview

**CraftPilot AI** is a comprehensive AI-powered SaaS platform designed for modern enterprises. It leverages a microservices architecture to deliver scalable, resilient, and high-performance AI solutions.

### 🎯 Main Goals

- **AI Integration**: Robust conversational experiences with LLMs like GPT and Claude
- **Visual AI**: Advanced image generation and processing capabilities
- **Enterprise Ready**: Enterprise-grade security, monitoring and scalability
- **Real-time**: Reactive programming for real-time data processing
- **Cloud Native**: Cloud-first approach with Kubernetes and Docker

---

## 🔐 Security

> **⚠️ WARNING**: This project currently contains development template configurations.

### 🚨 Pre-production Requirements

Before using this project in production, make sure to:

1. **📋 Read [SECURITY.md](./SECURITY.md)** - detailed security setup guide
2. **📄 Review [DEPLOYMENT_SECURITY.md](./DEPLOYMENT_SECURITY.md)** - production deployment security checklist
3. **🔑 Replace all placeholder passwords** (`CHANGE_ME_IN_PRODUCTION`)
4. **🔥 Configure Firebase service account**
5. **🗝️ Set up GitHub Secrets** (for CI/CD)
6. **🛡️ Configure SSL/TLS certificates**

### 🔐 Security Features

- **Firebase Authentication** - JWT token based authentication
- **Redis AUTH** - authenticated cache access
- **Role-based Access Control (RBAC)** - permission management
- **API Rate Limiting** - DDoS protection
- **Circuit Breaker Pattern** - failure isolation
- **Distributed Tracing** - security monitoring

### 📋 Security Checklist

- [ ] Create a `.env` from `.env.example` and change credentials
- [ ] Configure Firebase credentials
- [ ] Set repository secrets in GitHub
- [ ] Apply network security rules
- [ ] Configure SSL certificates
- [ ] Run security scanning

**⚠️ Do not run in production with default credentials!**

---

## ✨ Features

### 🤖 AI & Machine Learning

- **Large Language Models (LLM)** integrations
- **Image Generation** and processing services
- **User Memory Service** for personalized experiences
- **Analytics Service** for AI usage insights

### 🏢 Enterprise Features

- **Multi-tenant** architecture
- **Role-based** access control (RBAC)
- **Subscription Management** system
- **Credit-based** usage tracking
- **Real-time notifications**

### 🔧 Platform Features

- **Reactive Programming** (Spring WebFlux)
- **Event-driven** architecture (Kafka)
- **Circuit Breaker** pattern (Resilience4j)
- **API Gateway** for centralized routing
- **Service Discovery** (Eureka)
- **Distributed tracing** and monitoring

---

## 🏗️ Architecture

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

## 🚀 Services

### 🛡️ Core Infrastructure

|                Service | Port | Description                               | Technologies                        |
| ---------------------: | ---- | ----------------------------------------- | ----------------------------------- |
|        **API Gateway** | 8080 | Main entry point, routing, authentication | Spring Cloud Gateway, Firebase Auth |
|      **Eureka Server** | 8761 | Service discovery and registry            | Netflix Eureka                      |
| **CraftPilot Commons** | -    | Shared libraries and utilities            | Activity logging, Configuration     |

### 👤 Business Services

|                  Service | Port | Description                      | Features                                                |
| -----------------------: | ---- | -------------------------------- | ------------------------------------------------------- |
|         **User Service** | 8081 | User management and preferences  | Profile management, Preferences, Circuit breaker        |
|          **LLM Service** | 8082 | Language model integration       | OpenRouter API, Chat histories, Conversation management |
|        **Image Service** | 8083 | AI-assisted image generation     | Image generation, Processing, Storage                   |
| **Subscription Service** | 8084 | Subscription and plan management | Plan management, Billing, Usage tracking                |
|       **Credit Service** | 8085 | Credit system management         | Credit allocation, Usage monitoring, Limits             |

### 📊 Support Services

|                  Service | Port | Description         | Features                                        |
| -----------------------: | ---- | ------------------- | ----------------------------------------------- |
| **Notification Service** | 8086 | Notification system | Push notifications, Email, SMS                  |
|    **Analytics Service** | 8087 | Platform analytics  | Usage analytics, Reporting, Insights            |
|        **Admin Service** | 8088 | Admin panel         | User management, System monitoring, Admin tools |
|  **User Memory Service** | 8089 | User memory         | Context storage, Personalization                |
| **Activity Log Service** | 8090 | Activity tracking   | Audit logs, User activities, Compliance         |

### 🌊 Monitoring & DevOps

|                Service | Port | Description                              |
| ---------------------: | ---- | ---------------------------------------- |
| **Lighthouse Service** | 8091 | Health monitoring and lighthouse metrics |
|  **Lighthouse Worker** | -    | Background tasks and scheduled jobs      |

---

## 🛠️ Tech Stack

### ☕ Backend Framework

- **Java 21** - Modern Java features
- **Spring Boot 3.3.1** - Enterprise application framework
- **Spring Cloud 2023.0.2** - Microservices toolkit
- **Spring WebFlux** - Reactive programming
- **Maven** - Dependency management

### 🗄️ Database & Storage

- **Firebase/Firestore** - NoSQL document database
- **Redis** - In-memory caching and session storage
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

## 🚀 Setup

### 📋 Requirements

- **Java 21+**
- **Maven 3.8+**
- **Docker & Docker Compose**
- **Redis** (local or container)
- **Firebase Project** (for authentication)

### 1️⃣ Clone the project

```bash
git clone https://github.com/omerada/CraftPilot-API.git
cd CraftPilot-API
```

### 2️⃣ Environment Setup

```bash
# Place the Firebase configuration file
cp firebase-service-account.json scripts/

# Environment variables
export FIREBASE_PROJECT_ID=your-project-id
export REDIS_HOST=localhost
export REDIS_PORT=6379
```

### 3️⃣ Infrastructure Services

```bash
# Start Redis and the monitoring stack
docker-compose -f docker-compose.infra.yml up -d

# Check status
docker-compose -f docker-compose.infra.yml ps
```

### 4️⃣ Application Build

```bash
# Build all modules
mvn clean install

# Or package only
mvn clean package -DskipTests
```

### 5️⃣ Service Startup (Development)

```bash
# 1. Eureka Server
cd eureka-server && mvn spring-boot:run

# 2. API Gateway
cd api-gateway && mvn spring-boot:run

# 3. Core Services (run in parallel)
cd user-service && mvn spring-boot:run &
cd llm-service && mvn spring-boot:run &
cd image-service && mvn spring-boot:run &
```

### 🐳 Production with Docker

```bash
# Build all services
mvn clean package -DskipTests

# Build Docker images
docker build -t craftpilot/eureka-server eureka-server/
docker build -t craftpilot/api-gateway api-gateway/
docker build -t craftpilot/user-service user-service/
# ... other services

# Run with Docker Compose
docker-compose up -d
```

---

## 💻 Usage

### 🌐 Endpoints

|              Service | URL                                   | Notes                |
| -------------------: | ------------------------------------- | -------------------- |
|      **API Gateway** | http://localhost:8080                 | Main API endpoint    |
| **Eureka Dashboard** | http://localhost:8761                 | Service discovery UI |
|       **Swagger UI** | http://localhost:8080/swagger-ui.html | API documentation    |
|       **Prometheus** | http://localhost:9090                 | Metrics              |
|          **Grafana** | http://localhost:3000                 | Monitoring dashboard |

### 🔑 Authentication

```bash
# API call with Firebase Authentication token
curl -H "Authorization: Bearer YOUR_FIREBASE_TOKEN" \
     http://localhost:8080/api/users/profile
```

### 💬 LLM Service Usage

```bash
# Start a new conversation
curl -X POST http://localhost:8080/api/llm/conversations \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"title": "My Conversation"}'

# Send a message
curl -X POST http://localhost:8080/api/llm/conversations/{id}/messages \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"content": "Hello, AI!"}'
```

### 🖼️ Image Service Usage

```bash
# Image generation
curl -X POST http://localhost:8080/api/images/generate \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"prompt": "A beautiful sunset", "size": "1024x1024"}'
```

---

## 📚 API Documentation

### 🔍 Swagger UI

Interactive API docs for all services:

- **Main API**: http://localhost:8080/swagger-ui.html
- **User Service**: http://localhost:8081/swagger-ui.html
- **LLM Service**: http://localhost:8082/swagger-ui.html
- **Image Service**: http://localhost:8083/swagger-ui.html

### 📖 OpenAPI Specifications

```bash
# OpenAPI JSON specs
curl http://localhost:8080/v3/api-docs
curl http://localhost:8081/v3/api-docs
curl http://localhost:8082/v3/api-docs
```

### 🔐 Authentication

All API endpoints require a Firebase JWT token:

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

Using **Spring Cloud Sleuth** for request tracing:

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
# Run with hot reload in development
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

## 🤝 Contributing

### 📝 Contribution Guidelines

1. **Fork** the project
2. Create a **feature branch** (`git checkout -b feature/amazing-feature`)
3. **Commit** your changes (`git commit -m 'Add amazing feature'`)
4. **Push** (`git push origin feature/amazing-feature`)
5. Open a **Pull Request**

### 🐛 Bug Reports

Use the [Issue templates](../../issues/new/choose) to report bugs.

### 💡 Feature Requests

Share ideas via the [Discussions](../../discussions) section.

### 📋 Development Standards

- **Java Code Style**: Google Java Style Guide
- **Commit Convention**: Conventional Commits
- **Testing**: Minimum 80% test coverage
- **Documentation**: JavaDoc and API documentation

---

## 📄 License

This project is licensed under the **MIT License**. See the [LICENSE](LICENSE) file for details.

---

## 🙏 Thanks

- **Spring Team** - For the framework and ecosystem
- **Firebase Team** - For authentication and database services
- **OpenRouter** - LLM API provider
- **Contributors** - Open source community

---

## 📞 Contact

- **Website**: [craftpilot.io](https://craftpilot.io)
- **Email**: support@craftpilot.com
- **GitHub**: [CraftPilot Organization](https://github.com/craftpilot)
- **Discord**: [Community Server](https://discord.gg/craftpilot)

---

<div align="center">

**⭐ If you like the project, please give it a star!**

[🔝 Back to top](#-craftpilot-ai---saas-backend-platform)

</div>
