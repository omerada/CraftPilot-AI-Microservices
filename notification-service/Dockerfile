# Build stage
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /build

# Copy only pom files first for better caching
COPY pom.xml .
COPY notification-service/pom.xml notification-service/

# Install dependencies
RUN apk add --no-cache maven && \
    mvn dependency:go-offline -B -f notification-service/pom.xml

# Copy source code
COPY notification-service/src notification-service/src

# Build application
RUN mvn clean package -DskipTests -f notification-service/pom.xml

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

# JVM tuning for containers
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -Dspring.output.ansi.enabled=ALWAYS"

EXPOSE 8094
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]