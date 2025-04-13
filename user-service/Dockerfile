# Build stage
FROM maven:3.9.6-eclipse-temurin-21 AS build

WORKDIR /build
COPY pom.xml .
COPY src ./src

# Maven build
RUN mvn clean package -DskipTests

# Run stage
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app
COPY --from=build /build/target/*.jar app.jar

# Create logs directory and set permissions
RUN mkdir -p /app/logs && \
    chown -R 1000:1000 /app/logs && \
    chmod 755 /app/logs

# JVM settings
ENV JAVA_OPTS="-XX:+UseG1GC -XX:MaxGCPauseMillis=100 -XX:+UseStringDeduplication -Xmx1G -Xms512m"
ENV GOOGLE_APPLICATION_CREDENTIALS=/gcp-credentials.json

# Health check tools
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8060/actuator/health/liveness || exit 1

# Set non-root user
USER 1000:1000

EXPOSE 8060
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dspring.profiles.active=prod -jar app.jar"]