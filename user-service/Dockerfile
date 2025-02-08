# Build stage
FROM public.ecr.aws/amazoncorretto/amazoncorretto:21-al2023 AS build

WORKDIR /build

# Copy Maven wrapper files first
COPY .mvn/ .mvn/
COPY mvnw mvnw.cmd pom.xml ./

# Download dependencies first (this layer will be cached)
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY src ./src/

# Build the application
RUN ./mvnw clean package -DskipTests

# Run stage
FROM public.ecr.aws/amazoncorretto/amazoncorretto:21-al2023

WORKDIR /app

USER 1000

# Install required native dependencies
RUN yum update -y && \
    yum install -y glibc-devel openssl-devel apr-devel && \
    yum clean all && \
    rm -rf /var/cache/yum

COPY --from=build --chown=1000:1000 /build/target/*.jar app.jar

# Configure JVM for container and ARM64
ENV JAVA_OPTS="\
    -XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:InitialRAMPercentage=50.0 \
    -XX:+UseG1GC \
    -XX:MaxGCPauseMillis=200 \
    -XX:+UseStringDeduplication \
    -XX:+HeapDumpOnOutOfMemoryError \
    -XX:HeapDumpPath=/var/log/craftpilot/heap-dump.hprof \
    -Djava.security.egd=file:/dev/./urandom \
    -Dspring.output.ansi.enabled=ALWAYS \
    -Dio.netty.tryReflectionSetAccessible=true \
    -Dio.netty.transport.noNative=true"

ENV SERVER_PORT=8060

EXPOSE ${SERVER_PORT}

HEALTHCHECK --interval=30s --timeout=3s \
  CMD curl -f http://localhost:${SERVER_PORT}/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dserver.port=${SERVER_PORT} -Dspring.profiles.active=prod -jar app.jar"]