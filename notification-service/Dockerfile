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
    netcat \
    tzdata \
    libc6 \
    && rm -rf /var/lib/apt/lists/*

# Copy jar from build stage
COPY --from=builder /build/target/*.jar app.jar

# Create healthcheck script
COPY healthcheck.sh /healthcheck.sh
RUN chmod +x /healthcheck.sh

# Create directories with correct paths and permissions
RUN mkdir -p /app/config /app/logs && \
    mkdir -p /app/credentials && \
    chmod 755 /app/config /app/logs /app/credentials && \
    chown -R 1000:1000 /app

# Copy application files
COPY --chown=1000:1000 --from=builder /build/target/*.jar app.jar
COPY --chown=1000:1000 healthcheck.sh /healthcheck.sh
RUN chmod +x /healthcheck.sh

# Switch to non-root user
USER 1000

# Set environment variables
ENV SERVER_PORT=8053 \
    SPRING_PROFILES_ACTIVE=prod

# Healthcheck with increased intervals
HEALTHCHECK --interval=30s --timeout=10s --start-period=180s --retries=5 \
    CMD /healthcheck.sh

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
    --add-opens java.base/java.time=ALL-UNNAMED \
    --add-opens java.base/java.time.chrono=ALL-UNNAMED \
    -Djava.security.egd=file:/dev/./urandom \
    -Dserver.port=8053 \
    -Dspring.profiles.active=prod \
    -Duser.timezone=UTC"

EXPOSE 8053

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

