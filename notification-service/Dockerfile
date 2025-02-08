# Build stage - ARM64 specific
FROM --platform=linux/arm64 maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /build

# Copy maven files first
COPY pom.xml .
COPY .mvn/ .mvn/
COPY mvnw .
COPY mvnw.cmd .

# Set execute permissions for mvnw
RUN chmod +x mvnw

# Download dependencies
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY src/ src/

# Build application
RUN ./mvnw clean package -DskipTests

# Run stage - ARM64 specific
FROM --platform=linux/arm64 eclipse-temurin:21-jre-jammy
WORKDIR /app

# Add necessary tools and native libraries for ARM64
RUN apt-get update && apt-get install -y \
    curl \
    tzdata \
    libc6 \
    && rm -rf /var/lib/apt/lists/*

# Copy jar from build stage
COPY --from=builder /build/target/*.jar app.jar

# Create all necessary directories and set permissions
RUN mkdir -p /app/config /app/logs /app/firebase /gcp-credentials && \
    chown -R nobody:nogroup /app /gcp-credentials && \
    chmod 755 /app/config /app/logs /app/firebase /gcp-credentials

USER nobody

# Set environment variables
ENV SERVER_PORT=8053 \
    SPRING_PROFILES_ACTIVE=prod

# Healthcheck
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8053/actuator/health || exit 1

# Update JVM tuning with ARM64-optimized settings
ENV JAVA_OPTS="\
    -XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:+UseG1GC \
    -XX:+UseStringDeduplication \
    -XX:+ExitOnOutOfMemoryError \
    -Dfile.encoding=UTF-8 \
    -Dio.netty.noNative=true \
    -Dio.netty.noUnsafe=true \
    -Dio.netty.tryReflectionSetAccessible=true \
    -Dio.grpc.netty.shaded.io.netty.transport.noNative=true \
    -Dio.netty.transport.noNative=true \
    -Dio.netty.native.workdir=/tmp \
    -Djava.security.egd=file:/dev/./urandom \
    -Dserver.port=8053 \
    -Dspring.profiles.active=prod \
    -Duser.timezone=UTC"

EXPOSE 8053

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

