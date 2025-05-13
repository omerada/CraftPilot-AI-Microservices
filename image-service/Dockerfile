# Build stage
FROM maven:3.9.6-eclipse-temurin-21 AS build

WORKDIR /build
COPY pom.xml .
COPY src ./src

# Maven build
RUN mvn clean package -DskipTests

# Run stage
FROM eclipse-temurin:21-jre

WORKDIR /app

# Install necessary packages
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
    curl \
    netcat-openbsd \
    tzdata \
    libc6 \
    && rm -rf /var/lib/apt/lists/*

# Create necessary directories
RUN mkdir -p /tmp/netty /tmp/image-service

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

# Create healthcheck script
RUN echo '#!/bin/sh\ncurl -f http://localhost:8056/actuator/health || exit 1' > /healthcheck.sh && \
    chmod +x /healthcheck.sh

# Health check configuration
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD /healthcheck.sh

EXPOSE 8056
ENTRYPOINT ["java", "-jar", "app.jar"]