# Build stage
FROM eclipse-temurin:21-jdk-alpine AS builder
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

# Run stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Add necessary tools and native libraries
RUN apk add --no-cache \
    curl \
    tzdata \
    gcompat \
    libstdc++ \
    linux-headers \
    libc6-compat \
    && rm -rf /var/cache/apk/*

# Copy jar from build stage
COPY --from=builder /build/target/*.jar app.jar

# Create necessary directories with correct permissions
RUN mkdir -p /app/config /app/logs /gcp-credentials && \
    chmod 755 /app/config /app/logs /gcp-credentials

# Set environment variables
ENV SERVER_PORT=8053 \
    SPRING_PROFILES_ACTIVE=prod

# Healthcheck
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8053/actuator/health || exit 1

# Update JVM tuning with optimized settings
ENV JAVA_OPTS="\
    -XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:+UseG1GC \
    -XX:+UseStringDeduplication \
    -XX:+ExitOnOutOfMemoryError \
    -Dio.netty.tryReflectionSetAccessible=true \
    -Dio.netty.noUnsafe=true \
    -Dfile.encoding=UTF-8 \
    -Dio.netty.noPreferDirect=true \
    -Dio.netty.recycler.maxCapacity=0 \
    -Dio.netty.transport.noNative=true \
    -Dio.netty.nativeEpoll=false \
    -Djava.security.egd=file:/dev/./urandom \
    -Dserver.port=8053 \
    -Dspring.profiles.active=prod \
    -Duser.timezone=UTC"

EXPOSE 8053

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

