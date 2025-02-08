# Build stage - ARM64 specific
FROM --platform=linux/arm64 maven:3.9.6-eclipse-temurin-21 AS build

WORKDIR /build
COPY pom.xml .
RUN mvn dependency:go-offline

COPY src ./src
RUN mvn clean package -DskipTests

# Run stage - ARM64 specific
FROM --platform=linux/arm64 eclipse-temurin:21-jre-jammy

WORKDIR /app
COPY --from=build /build/target/*.jar app.jar

# Add necessary tools and native libraries for ARM64
RUN apt-get update && apt-get install -y \
    curl \
    tzdata \
    libc6 \
    && rm -rf /var/lib/apt/lists/*

# GCP credentials için volume mount noktası
RUN mkdir -p /gcp-credentials && chmod 700 /gcp-credentials

# JVM ve Netty ayarları
ENV JAVA_OPTS="\
    -XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:InitialRAMPercentage=50.0 \
    -XX:+UseG1GC \
    -XX:+UseStringDeduplication \
    -XX:+ExitOnOutOfMemoryError \
    -Dio.netty.tryReflectionSetAccessible=true \
    -Dio.netty.noUnsafe=true \
    -Dio.netty.noPreferDirect=true \
    -Dio.netty.recycler.maxCapacity=0 \
    -Dio.netty.transport.noNative=true \
    -Dio.netty.nativeEpoll=false \
    -Dio.grpc.netty.shaded.io.netty.transport.noNative=true \
    -Dio.netty.native.workdir=/tmp \
    -Dio.netty.native.try.embedded=false \
    -Djava.security.egd=file:/dev/./urandom \
    -Dspring.output.ansi.enabled=ALWAYS"

HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8052/actuator/health || exit 1

EXPOSE 8052
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]