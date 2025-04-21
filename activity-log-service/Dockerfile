# Build stage
FROM maven:3.9.6-eclipse-temurin-17 AS build

WORKDIR /build
COPY pom.xml .
COPY src ./src

# Maven build
RUN mvn clean package -DskipTests

# Run stage
FROM eclipse-temurin:17-jre

WORKDIR /app
COPY --from=build /build/target/*.jar app.jar

# JVM settings
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=70.0 -XX:+UseG1GC -Djava.security.egd=file:/dev/./urandom"
ENV GOOGLE_APPLICATION_CREDENTIALS=/gcp-credentials.json

# Install curl for healthcheck
RUN apt-get update && \
    apt-get install -y --no-install-recommends curl && \
    rm -rf /var/lib/apt/lists/*

# Simple healthcheck configuration
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8095/actuator/health || exit 1

EXPOSE 8095

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
