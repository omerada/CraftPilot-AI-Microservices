# Build stage
FROM maven:3.9.6-eclipse-temurin-21 AS build

WORKDIR /build
COPY pom.xml .
COPY src ./src
COPY .mvn ./.mvn
COPY mvnw .
COPY mvnw.cmd .

# Maven build
RUN mvn clean package -DskipTests

# Run stage
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# Gerekli dizinleri oluştur
RUN mkdir -p /app/config

# JAR dosyasını kopyala
COPY --from=build /build/target/*.jar app.jar

# Health check için curl ekle
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# JVM ayarları
ENV JAVA_OPTS="-XX:+UseG1GC -XX:MaxGCPauseMillis=100 -XX:+UseStringDeduplication -Xmx1G -Xms512m"

# Sağlık kontrolü
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8065/actuator/health || exit 1

EXPOSE 8065
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]