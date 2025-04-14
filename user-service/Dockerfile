# Redis Client Library Build Stage
FROM maven:3.9.6-eclipse-temurin-21 AS redis-lib-build

# Clone/copy the redis-client-lib source
WORKDIR /build-lib
COPY redis-client-lib ./redis-client-lib

# Build and install the redis-client-lib to local Maven repository
RUN cd redis-client-lib && mvn clean install -DskipTests

# User Service Build stage
FROM maven:3.9.6-eclipse-temurin-21 AS build

WORKDIR /build
COPY pom.xml .
COPY src ./src

# Copy Maven local repository from previous stage
COPY --from=redis-lib-build /root/.m2 /root/.m2

# Maven build
RUN mvn clean package -DskipTests

# Run stage
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app
COPY --from=build /build/target/*.jar app.jar

# Create logs directory and set permissions
RUN mkdir -p /app/logs && \
    chown -R 1000:1000 /app/logs && \
    chmod 755 /app/logs

# JVM settings
ENV JAVA_OPTS="-XX:+UseG1GC -XX:MaxGCPauseMillis=100 -XX:+UseStringDeduplication -Xmx1G -Xms512m"
ENV GOOGLE_APPLICATION_CREDENTIALS=/gcp-credentials.json

# Health check tools
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8060/actuator/health/liveness || exit 1

# Set non-root user
USER 1000:1000

EXPOSE 8060
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dspring.profiles.active=prod -jar app.jar"]