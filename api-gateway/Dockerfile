# Build stage
FROM maven:3.9.6-eclipse-temurin-21 AS builder

WORKDIR /build
COPY . .
RUN chmod +x ./mvnw
RUN mvn clean package -DskipTests

# Run stage
FROM openjdk:21-slim

WORKDIR /app
COPY --from=builder /build/target/*.jar app.jar

# Create config directory
RUN mkdir -p /app/config

# JVM options
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

# Default environment variables
ENV SERVER_PORT=8080 \
    SPRING_PROFILES_ACTIVE=prod \
    SPRING_APPLICATION_NAME=api-gateway \
    SPRING_CLOUD_GATEWAY_DISCOVERY_LOCATOR_ENABLED=true \
    SPRING_CLOUD_GATEWAY_DISCOVERY_LOCATOR_LOWER_CASE_SERVICE_ID=true \
    FIREBASE_CREDENTIALS_PATH=/app/config/firebase-credentials.json

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]