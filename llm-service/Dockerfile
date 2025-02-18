# Build stage
FROM --platform=linux/arm64 maven:3.9.6-eclipse-temurin-21 AS build

WORKDIR /build
COPY pom.xml .
COPY src ./src

# Build with specific platform
RUN mvn clean package -DskipTests

# Run stage
FROM --platform=linux/arm64 eclipse-temurin:21-jre-alpine

WORKDIR /app

# Install required packages
RUN apk update && \
    apk add --no-cache curl tzdata libc6-compat libstdc++ && \
    rm -rf /var/cache/apk/*

# Create necessary directories
RUN mkdir -p /tmp/netty /app/native-lib

# Copy application
COPY --from=build /build/target/*.jar app.jar

# Set environment variables for native library handling
ENV LD_LIBRARY_PATH=/app/native-lib
ENV JAVA_OPTS="-XX:+UseContainerSupport \
               -XX:MaxRAMPercentage=75.0 \
               -XX:MinRAMPercentage=50.0 \
               -XX:+UseG1GC \
               -XX:+HeapDumpOnOutOfMemoryError \
               -XX:HeapDumpPath=/app/heapdump.hprof \
               -Dio.netty.noNative=true \
               -Dio.netty.tryReflectionSetAccessible=false \
               -Dnetty.native.workdir=/tmp/netty"

# Health check with appropriate timing
HEALTHCHECK --interval=30s --timeout=10s --start-period=120s --retries=3 \
  CMD curl -f http://localhost:8062/actuator/health || exit 1

EXPOSE 8062

# Start application with proper configuration
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -Dspring.profiles.active=prod -jar app.jar"]