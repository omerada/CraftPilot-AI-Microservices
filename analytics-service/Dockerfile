# Build stage
FROM maven:3.9.6-eclipse-temurin-21 AS build

WORKDIR /app

# Cache Maven dependencies
COPY pom.xml ./
RUN mvn dependency:go-offline

# Build the application
COPY src ./src/
RUN mvn clean package -DskipTests

# Run stage
FROM openjdk:21-slim

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

# Install curl for healthcheck and diagnostics
RUN apt-get update && \
    apt-get install -y --no-install-recommends curl netcat-openbsd jq && \
    rm -rf /var/lib/apt/lists/*

# MongoDB connection - Default olarak güvenli varsayılan değerler
ENV MONGODB_URI=mongodb://craftpilot:secure_password@mongodb:27017/analytics?authSource=admin&retryWrites=true&w=majority&serverSelectionTimeoutMS=60000
ENV MONGODB_DATABASE=analytics
ENV MONGODB_USERNAME=craftpilot
ENV MONGODB_PASSWORD=secure_password
ENV MONGODB_HOST=mongodb
ENV MONGODB_PORT=27017

# Kafka configuration - Fail-fast olmadan yapılandırma
ENV KAFKA_BOOTSTRAP_SERVERS=kafka:9092
ENV KAFKA_ENABLED=false
ENV KAFKA_ADMIN_AUTO_CREATE=false

# Eureka configuration
ENV EUREKA_SERVER_URL=http://craftpilot:13579ada@eureka-server:8761/eureka/

# Spring and application configs
ENV SPRING_PROFILES_ACTIVE=prod
ENV SERVER_PORT=8064
ENV MONGODB_AUTO_INDEX_CREATION=false
ENV SPRING_DATA_MONGODB_AUTO_INDEX_CREATION=false

# Create healthcheck script
COPY <<EOF /app/healthcheck.sh
#!/bin/bash
set -e

# Basic health check
HEALTH_ENDPOINT="http://localhost:${SERVER_PORT:-8064}/actuator/health"
echo "Checking health at: $HEALTH_ENDPOINT"

# Try with curl
if curl -f $HEALTH_ENDPOINT > /dev/null 2>&1; then
  echo "Health check passed"
  exit 0
else
  echo "Health check failed"
  exit 1
fi
EOF

RUN chmod +x /app/healthcheck.sh

# Healthcheck for container orchestration using the script
HEALTHCHECK --interval=30s --timeout=10s --start-period=120s --retries=5 \
  CMD /app/healthcheck.sh

EXPOSE 8064

ENTRYPOINT ["java", "-Xms512m", "-Xmx1g", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", \
  "-Dfile.encoding=UTF-8", "-Djava.security.egd=file:/dev/./urandom", \
  "-Dspring.data.mongodb.auto-index-creation=false", \
  "-jar", "app.jar"]