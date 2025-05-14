# Redis Client Library Build Stage
FROM maven:3.9.6-eclipse-temurin-21 AS redis-lib-build

# Clone/copy the redis-client-lib source
WORKDIR /build-lib
COPY redis-client-lib ./redis-client-lib

# Build and install the redis-client-lib to local Maven repository
RUN cd redis-client-lib && mvn clean install -DskipTests

# Build stage
FROM maven:3.9.6-eclipse-temurin-21 AS builder

WORKDIR /build

# Copy Redis client lib first
COPY redis-client-lib/ /build/redis-client-lib/

# Build Redis client lib
RUN cd /build/redis-client-lib && mvn clean install -DskipTests

# Copy user-service
COPY user-service/ /build/user-service/

# Build user-service
RUN cd /build/user-service && mvn clean package -DskipTests

# Run stage
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy jar file
COPY --from=builder /build/user-service/target/*.jar app.jar

# MongoDB için çalışma zamanı ayarları
ENV SPRING_PROFILES_ACTIVE=prod
ENV SPRING_DATA_MONGODB_AUTO_INDEX_CREATION=false
ENV SPRING_APPLICATION_MONGODB_URI=""
ENV SPRING_APPLICATION_MONGODB_DATABASE=user-service
ENV GOOGLE_APPLICATION_CREDENTIALS=/app/config/firebase-credentials.json

# Health check ekliyorum
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget -q --no-check-certificate -O /dev/null http://localhost:8090/actuator/health || exit 1

# Firebase için gerekli konfigürasyon dizini oluştur
RUN mkdir -p /app/config
VOLUME /app/config

EXPOSE 8090

ENTRYPOINT ["java", "-jar", "app.jar"]