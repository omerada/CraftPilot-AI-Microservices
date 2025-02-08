# Build stage
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /build

# Copy root pom.xml
COPY ./pom.xml .

# Copy service specific files
COPY ./notification-service/pom.xml ./notification-service/
COPY ./notification-service/src ./notification-service/src

# Build with Maven - only notification-service module
RUN apk add --no-cache maven && \
    mvn clean package -DskipTests \
    -pl notification-service \
    -am \
    -Dspring.profiles.active=prod \
    -Dmaven.repo.local=/root/.m2/repository

# Run stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Add curl for health check
RUN apk add --no-cache curl

# Copy jar from build stage
COPY --from=builder /build/notification-service/target/*.jar app.jar

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
  CMD curl -f http://localhost:8094/actuator/health || exit 1

# JVM tuning
ENV JAVA_OPTS="\
    -XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:+UseG1GC \
    -XX:+UseStringDeduplication \
    -XX:+ExitOnOutOfMemoryError \
    -Dspring.output.ansi.enabled=ALWAYS"

EXPOSE 8094
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]