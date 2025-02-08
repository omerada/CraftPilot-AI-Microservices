# Build stage
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /build

# Copy pom.xml first
COPY pom.xml ./

# Install dependencies
RUN apk add --no-cache maven

# Download dependencies
RUN mvn dependency:go-offline -B

# Copy source
COPY src ./src

# Build
RUN mvn clean package -DskipTests

# Run stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Add necessary tools
RUN apk add --no-cache curl tzdata && \
    cp /usr/share/zoneinfo/UTC /etc/localtime && \
    echo "UTC" > /etc/timezone

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

# JVM tuning
ENV JAVA_OPTS="\
    -XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:+UseG1GC \
    -XX:+UseStringDeduplication \
    -XX:+ExitOnOutOfMemoryError \
    -Djava.security.egd=file:/dev/./urandom \
    -Dserver.port=8053 \
    -Dspring.profiles.active=prod \
    -Duser.timezone=UTC"

EXPOSE 8053

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

