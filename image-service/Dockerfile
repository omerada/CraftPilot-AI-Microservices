# Build stage
FROM maven:3.9.6-eclipse-temurin-21 AS build

WORKDIR /build
COPY pom.xml .
COPY src ./src

# Maven build
RUN mvn clean package -DskipTests

# Run stage
FROM eclipse-temurin:21-jre

WORKDIR /app

# Install necessary packages
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
    curl \
    netcat-openbsd \
    dnsutils \
    iputils-ping \
    tzdata \
    libc6 \
    && rm -rf /var/lib/apt/lists/*

# Create necessary directories
RUN mkdir -p /tmp/netty /tmp/image-service

# Copy application
COPY --from=build /build/target/*.jar app.jar

# Create startup script with dependency checks
RUN echo '#!/bin/bash \n\
set -e \n\
\n\
echo "Starting Image Service..." \n\
echo "Java version: $(java -version 2>&1 | head -n 1)" \n\
\n\
# Check MongoDB connection \n\
echo "Testing connection to MongoDB..." \n\
for i in {1..30}; do \n\
  if nc -z -w 2 mongodb 27017; then \n\
    echo "✅ Successfully connected to MongoDB" \n\
    break \n\
  fi \n\
  echo "⏳ Waiting for MongoDB to be available... ($i/30)" \n\
  if [ $i -eq 30]; then \n\
    echo "⚠️ Could not connect to MongoDB within timeout, but will continue startup" \n\
  fi \n\
  sleep 2 \n\
done \n\
\n\
# Check Kafka connection \n\
echo "Testing connection to Kafka..." \n\
for i in {1..15}; do \n\
  if nc -z -w 2 kafka 9092; then \n\
    echo "✅ Successfully connected to Kafka" \n\
    break \n\
  fi \n\
  echo "⏳ Waiting for Kafka to be available... ($i/15)" \n\
  if [ $i -eq 15]; then \n\
    echo "⚠️ Could not connect to Kafka within timeout, but will continue startup" \n\
  fi \n\
  sleep 2 \n\
done \n\
\n\
echo "Starting application with options: $JAVA_OPTS" \n\
exec java $JAVA_OPTS -jar app.jar \n\
' > /startup.sh
RUN chmod +x /startup.sh

# Set environment variables
ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_TOOL_OPTIONS="\
    -XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:+UseG1GC \
    -Dio.netty.tryReflectionSetAccessible=true \
    -Djava.security.egd=file:/dev/./urandom \
    -Djava.awt.headless=true \
    -Dfile.encoding=UTF-8"

# Create healthcheck script
RUN echo '#!/bin/sh\ncurl -f http://localhost:8056/actuator/health || exit 1' > /healthcheck.sh && \
    chmod +x /healthcheck.sh

# Health check configuration
HEALTHCHECK --interval=30s --timeout=5s --start-period=120s --retries=3 \
  CMD /healthcheck.sh

EXPOSE 8056
ENTRYPOINT ["/startup.sh"]