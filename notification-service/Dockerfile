# Build stage
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /build

# Copy only notification service files
COPY notification-service/pom.xml pom.xml
COPY notification-service/src src

# Build with Maven
RUN apk add --no-cache maven && \
    mvn clean package \
    -DskipTests \
    -Dspring.profiles.active=prod \
    -Dmaven.repo.local=/root/.m2/repository

# Run stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Add curl for health check
RUN apk add --no-cache curl

# Copy jar from build stage
COPY --from=builder /build/target/*.jar app.jar

# Copy firebase-service-account.json
COPY notification-service/src/main/resources/firebase-service-account.json /app/firebase-service-account.json

# Create config directory
RUN mkdir -p /app/config

# GCP credentials için volume mount noktası
RUN mkdir -p /gcp-credentials

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
  CMD curl -f http://localhost:8094/actuator/health || exit 1

# JVM tuning
ENV JAVA_OPTS="\
    -XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:+UseG1GC \
    -XX:+UseStringDeduplication \
    -XX:+ExitOnOutOfMemoryError \
    -Dspring.output.ansi.enabled=ALWAYS"

EXPOSE 8094
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]