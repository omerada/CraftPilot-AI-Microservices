# Build stage - sadece ARM64
FROM --platform=linux/arm64 maven:3.9.6-eclipse-temurin-21 AS build

WORKDIR /build
COPY pom.xml .
# Maven bağımlılıklarını önbelleğe almak için
RUN mvn dependency:go-offline

COPY src ./src
# Maven build için platform ayarı
ARG TARGETARCH=arm64
RUN echo "Building for ARM64 architecture"
RUN mvn clean package -DskipTests

# Run stage - sadece ARM64
FROM --platform=linux/arm64 eclipse-temurin:21-jre-jammy

WORKDIR /app
COPY --from=build /build/target/*.jar app.jar

# GCP credentials için volume mount noktası
RUN mkdir -p /gcp-credentials && chmod 700 /gcp-credentials

# JVM ayarları ve sistem optimizasyonları
ENV JAVA_OPTS="-XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:InitialRAMPercentage=50.0 \
    -XX:+UseG1GC \
    -XX:+UseStringDeduplication \
    -XX:+ExitOnOutOfMemoryError \
    -Djava.security.egd=file:/dev/./urandom \
    -Dserver.port=8092 \
    -Dspring.profiles.active=prod \
    -Deureka.instance.preferIpAddress=true \
    -Dspring.cloud.service-registry.auto-registration.enabled=true \
    -Dspring.output.ansi.enabled=ALWAYS"

# Healthcheck yapılandırması
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8092/actuator/health || exit 1

EXPOSE 8092
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]