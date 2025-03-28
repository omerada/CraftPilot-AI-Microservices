# Build stage
FROM --platform=linux/arm64 maven:3.9.6-eclipse-temurin-21 AS build

WORKDIR /build
COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests

# Run stage
FROM --platform=linux/arm64 eclipse-temurin:21-jre

WORKDIR /app

# Install required packages
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
    curl \
    tzdata \
    libc6 \
    libstdc++6 \
    && rm -rf /var/lib/apt/lists/*

# Create necessary directories
RUN mkdir -p /tmp/netty /app/native-lib

# Copy application
COPY --from=build /build/target/*.jar app.jar

# Set environment variables
ENV JAVA_TOOL_OPTIONS="\
    -XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:+UseG1GC \
    -Dio.netty.tryReflectionSetAccessible=true \
    -Djavax.net.debug=ssl \
    -Dio.netty.leakDetection.level=disabled \
    -Djava.security.egd=file:/dev/./urandom \
    -Djava.awt.headless=true \
    -Dfile.encoding=UTF-8 \
    --add-opens=java.base/java.nio=ALL-UNNAMED \
    --add-opens=java.base/sun.nio.ch=ALL-UNNAMED \
    --add-opens=java.base/java.lang=ALL-UNNAMED"

# Google Cloud credential yapılandırması - CI/CD ile uyumlu hale getirildi
ENV GOOGLE_APPLICATION_CREDENTIALS=/gcp-credentials.json

# Service account anahtarını container'a kopyala (CI/CD ile uyumlu yol)
COPY serviceAccountKey.json /gcp-credentials.json

# Health check düzeltmesi
HEALTHCHECK --interval=10s --timeout=5s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8062/health || curl -f http://localhost:8062/actuator/health || exit 1

EXPOSE 8062

ENTRYPOINT ["java", "-jar", "app.jar"]