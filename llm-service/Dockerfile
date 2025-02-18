# Build stage
FROM --platform=linux/arm64 maven:3.9.6-eclipse-temurin-21 AS build

WORKDIR /build
COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests

# Run stage
FROM --platform=linux/arm64 eclipse-temurin:21-jre-alpine

WORKDIR /app

# Install required packages
RUN apk update && \
    apk add --no-cache curl tzdata libc6-compat libstdc++ gcompat && \
    rm -rf /var/cache/apk/*

# Create necessary directories
RUN mkdir -p /tmp/netty /app/native-lib

# Copy application
COPY --from=build /build/target/*.jar app.jar

# Set environment variables
ENV JAVA_TOOL_OPTIONS="\
    -XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:+UseG1GC \
    -XX:+HeapDumpOnOutOfMemoryError \
    -XX:HeapDumpPath=/app/heapdump.hprof \
    -Dio.netty.noNative=false \
    -Dio.netty.noUnsafe=false \
    -Dio.netty.tryReflectionSetAccessible=true \
    -Dio.netty.leakDetection.level=DISABLED \
    -Djava.io.tmpdir=/tmp \
    -Dnetty.native.workdir=/tmp/netty \
    -Dio.netty.native.workdir=/tmp/netty \
    --add-opens=java.base/java.nio=ALL-UNNAMED \
    --add-opens=java.base/jdk.internal.misc=ALL-UNNAMED \
    --add-opens=java.base/sun.nio.ch=ALL-UNNAMED \
    --add-exports=java.base/jdk.internal.misc=ALL-UNNAMED \
    --add-exports=java.base/sun.nio.ch=ALL-UNNAMED"

HEALTHCHECK --interval=30s --timeout=10s --start-period=120s --retries=3 \
  CMD curl -f http://localhost:8062/actuator/health || exit 1

EXPOSE 8062

ENTRYPOINT ["java", "-jar", "app.jar"]