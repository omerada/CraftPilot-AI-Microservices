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

# JVM ayarları
ENV JAVA_OPTS="-XX:+UseG1GC -XX:MaxGCPauseMillis=100 -XX:+UseStringDeduplication -Xmx1G -Xms512m"
ENV GOOGLE_APPLICATION_CREDENTIALS=/gcp-credentials.json

# Health check için curl ekle
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

HEALTHCHECK --interval=10s --timeout=3s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8062/actuator/health/liveness || exit 1

EXPOSE 8062
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dspring.profiles.active=prod -jar app.jar"]