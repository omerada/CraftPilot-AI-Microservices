# Build stage
FROM maven:3.9.6-eclipse-temurin-17-alpine AS build

WORKDIR /build
# Maven dependency önbelleğe alma
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Kaynak kodları kopyalama ve build
COPY src ./src
RUN mvn clean package -DskipTests

# Run stage
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app
COPY --from=build /build/target/*.jar app.jar

# JVM ayarları optimizasyonu
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=70.0 -XX:+UseG1GC -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom"
ENV GOOGLE_APPLICATION_CREDENTIALS=/gcp-credentials.json

# Spring Boot actuator API'sini kullanan sağlık kontrolü
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=5 \
  CMD wget -q --spider http://localhost:8095/actuator/health || exit 1

EXPOSE 8095

# Uygulama kullanıcısı - güvenlik için
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
