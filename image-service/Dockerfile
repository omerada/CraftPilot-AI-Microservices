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
# Check environment variables \n\
if [ -z "$SPRING_DATA_MONGODB_URI" ]; then \n\
  echo "❌ Warning: SPRING_DATA_MONGODB_URI is not set, using default value" \n\
else \n\
  echo "✅ SPRING_DATA_MONGODB_URI is configured" \n\
fi \n\
\n\
# Extract MongoDB host and port from URI \n\
if [[ "$SPRING_DATA_MONGODB_URI" =~ mongodb://[^:]*:[^@]*@([^:]+):([0-9]+) ]]; then \n\
  MONGODB_HOST="${BASH_REMATCH[1]}" \n\
  MONGODB_PORT="${BASH_REMATCH[2]}" \n\
  echo "📊 Extracted MongoDB host: $MONGODB_HOST port: $MONGODB_PORT" \n\
else \n\
  MONGODB_HOST="mongodb" \n\
  MONGODB_PORT="27017" \n\
  echo "⚠️ Could not extract MongoDB host and port from URI, using defaults" \n\
fi \n\
\n\
# Check MongoDB connection \n\
echo "🔍 Testing connection to MongoDB on $MONGODB_HOST:$MONGODB_PORT..." \n\
for i in {1..15}; do \n\
  if nc -z -w 2 $MONGODB_HOST $MONGODB_PORT; then \n\
    echo "✅ Successfully connected to MongoDB" \n\
    break \n\
  fi \n\
  echo "⏳ Waiting for MongoDB to be available... ($i/15)" \n\
  if [ $i -eq 15 ]; then \n\
    echo "⚠️ Could not connect to MongoDB within timeout, but will continue startup" \n\
  fi \n\
  sleep 2 \n\
done \n\
\n\
# Check Kafka connection \n\
echo "🔍 Testing connection to Kafka..." \n\
for i in {1..10}; do \n\
  if nc -z -w 2 kafka 9092; then \n\
    echo "✅ Successfully connected to Kafka" \n\
    break \n\
  fi \n\
  echo "⏳ Waiting for Kafka to be available... ($i/10)" \n\
  if [ $i -eq 10 ]; then \n\
    echo "⚠️ Could not connect to Kafka within timeout, but will continue startup" \n\
  fi \n\
  sleep 2 \n\
done \n\
\n\
# Check Redis connection \n\
echo "🔍 Testing connection to Redis..." \n\
for i in {1..10}; do \n\
  if nc -z -w 2 redis 6379; then \n\
    echo "✅ Successfully connected to Redis" \n\
    break \n\
  fi \n\
  echo "⏳ Waiting for Redis to be available... ($i/10)" \n\
  if [ $i -eq 10 ]; then \n\
    echo "⚠️ Could not connect to Redis within timeout, but will continue startup" \n\
  fi \n\
  sleep 2 \n\
done \n\
\n\
# Check Eureka connection \n\
echo "🔍 Testing connection to Eureka..." \n\
for i in {1..10}; do \n\
  if nc -z -w 2 eureka-server 8761; then \n\
    echo "✅ Successfully connected to Eureka" \n\
    break \n\
  fi \n\
  echo "⏳ Waiting for Eureka to be available... ($i/10)" \n\
  if [ $i -eq 10 ]; then \n\
    echo "⚠️ Could not connect to Eureka within timeout, but will continue startup" \n\
  fi \n\
  sleep 2 \n\
done \n\
\n\
echo "🚀 Starting application with options: $JAVA_OPTS" \n\
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