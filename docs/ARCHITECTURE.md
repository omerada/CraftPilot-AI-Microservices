# 🏗️ CraftPilot AI - Architecture Overview

## 📊 System Architecture

```
Frontend/Mobile → API Gateway → Microservices → Database/Cache
```

## 🚪 API Gateway (Port: 8080)

- **Firebase Authentication** for security
- **Rate limiting** and throttling
- **Load balancing** and routing
- Single entry point for all services

## 🗺️ Service Discovery (Port: 8761)

- **Eureka Server** for service registry
- Automatic service discovery
- Health check and monitoring

## 🔧 Core Services

### 👤 User Service (8081)

- User profile management
- Firebase integration
- Circuit breaker pattern

### 🤖 LLM Service (8082)

- OpenRouter API integration
- Conversation history
- AI model management

### 🖼️ Image Service (8083)

- AI image generation
- Image processing
- Cloud storage

### 💳 Subscription Service (8084)

- Plan management
- Billing
- Usage tracking

### 💰 Credit Service (8085)

- Credit system
- Usage limits
- Resource monitoring

## 🔔 Support Services

### 📊 Analytics Service (8087)

- Usage analytics
- Reporting
- Business intelligence

### 🛡️ Admin Service (8088)

- Admin panel
- System monitoring
- User management

### 🧠 User Memory Service (8089)

- Context storage
- Personalization
- User preferences

### 📝 Activity Log Service (8090)

- Audit logs
- User activities
- Compliance tracking

## 🗄️ Data Layer

### 🔥 Firebase/Firestore

- NoSQL document database
- Real-time updates
- Authentication

### 📦 Redis

- In-memory caching
- Session storage
- Performance optimization

## 📨 Communication

### Synchronous

- REST APIs (Spring WebFlux)
- Service-to-service calls

### Asynchronous

- Kafka message broker
- Event-driven architecture
- Real-time notifications

## 🔐 Security

- **Firebase JWT** authentication
- **Role-based** access control
- **Circuit breaker** pattern
- **Rate limiting** protection

## 📊 Monitoring

- **Prometheus** metrics
- **Grafana** dashboards
- **Health checks**
- **Distributed tracing**
