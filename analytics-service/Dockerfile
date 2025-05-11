# Build stage
FROM maven:3.9.6-eclipse-temurin-21 AS build

WORKDIR /build
COPY pom.xml .
COPY src ./src

# Maven build
RUN mvn clean package -DskipTests

# Run stage
FROM eclipse-temurin:21-jdk

WORKDIR /app
COPY --from=build /build/target/*.jar app.jar

# Install curl for health checks
RUN apt-get update && \
    apt-get install -y --no-install-recommends curl && \
    rm -rf /var/lib/apt/lists/*

# JVM ayarları
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

# Create directories
RUN mkdir -p /app/config /app/credentials

# Health check configuration
HEALTHCHECK --interval=30s --timeout=10s --start-period=120s --retries=5 \
  CMD curl -f http://localhost:8064/actuator/health || exit 1

EXPOSE 8064
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]