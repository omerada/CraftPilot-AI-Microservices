# Build stage
FROM maven:3.9.6-eclipse-temurin-17 AS build

WORKDIR /build
COPY pom.xml .
# Maven wrapper'ı kopyalamak yerine doğrudan Maven kullan
COPY src ./src

# Doğrudan Maven komutunu kullan
RUN mvn clean package -DskipTests

# Run stage
FROM eclipse-temurin:17-jre

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
RUN mkdir -p /tmp/netty /tmp/lighthouse

# Copy application
COPY --from=build /build/target/*.jar app.jar

# Set environment variables
ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_TOOL_OPTIONS="\
    -XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:+UseG1GC \
    -Dio.netty.tryReflectionSetAccessible=true \
    -Djava.security.egd=file:/dev/./urandom \
    -Djava.awt.headless=true \
    -Dfile.encoding=UTF-8"

# Health check configuration
HEALTHCHECK --interval=10s --timeout=5s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8085/health || curl -f http://localhost:8085/actuator/health || exit 1

EXPOSE 8085
ENTRYPOINT ["java", "-jar", "app.jar"]