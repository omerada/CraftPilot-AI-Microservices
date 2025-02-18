# Build stage
FROM --platform=linux/arm64 maven:3.9.6-eclipse-temurin-21 AS build

WORKDIR /build
COPY pom.xml .
COPY src ./src

# Maven build with specific platform
RUN mvn clean package -DskipTests

# Run stage
FROM --platform=linux/arm64 eclipse-temurin:21-jre-alpine

WORKDIR /app

ARG OPENROUTER_API_KEY
ENV OPENROUTER_API_KEY=${OPENROUTER_API_KEY}

COPY --from=build /build/target/*.jar app.jar

# JVM ve Netty ayarları
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

ENV GOOGLE_APPLICATION_CREDENTIALS=/gcp-credentials.json

# Alpine paketlerini güncelle ve curl ekle
RUN apk update && \
    apk add --no-cache curl tzdata && \
    rm -rf /var/cache/apk/*

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=120s --retries=3 \
  CMD curl -f http://localhost:8062/actuator/health || exit 1

EXPOSE 8062

# Java runtime flags ile başlat
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -Dspring.profiles.active=prod -jar app.jar"]