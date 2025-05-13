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

# Install necessary packages for network diagnostics and health checks
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
    curl \
    netcat-openbsd \
    dnsutils \
    iputils-ping \
    tzdata \
    libc6 \
    jq \
    && rm -rf /var/lib/apt/lists/*

# Create necessary directories
RUN mkdir -p /tmp/netty /tmp/image-service /gcp-credentials /data/db

# Copy application
COPY --from=build /build/target/*.jar app.jar

# Create improved startup script with more robust dependency checks
RUN echo '#!/bin/bash \n\
set -e \n\
\n\
echo "Starting Image Service..." \n\
echo "Java version: $(java -version 2>&1 | head -n 1)" \n\
\n\
# Extract MongoDB host and credentials from URI \n\
if [[ "$SPRING_DATA_MONGODB_URI" =~ mongodb://([^:]*):([^@]*)@([^:/]+) ]]; then \n\
  MONGODB_USERNAME="${BASH_REMATCH[1]}" \n\
  MONGODB_PASSWORD="${BASH_REMATCH[2]}" \n\
  MONGODB_HOST="${BASH_REMATCH[3]}" \n\
  echo "📊 Extracted MongoDB details - User: $MONGODB_USERNAME, Host: $MONGODB_HOST" \n\
else \n\
  # Fallback to environment variables or defaults \n\
  MONGODB_USERNAME=${MONGO_ROOT_USERNAME:-craftpilot} \n\
  MONGODB_PASSWORD=${MONGO_ROOT_PASSWORD:-secure_password} \n\
  MONGODB_HOST=${MONGODB_HOST:-craftpilot-mongodb} \n\
  echo "⚠️ Using configured MongoDB details - User: $MONGODB_USERNAME, Host: $MONGODB_HOST" \n\
fi \n\
\n\
# Check MongoDB connection - more retries and using extracted host \n\
echo "🔍 Testing connection to MongoDB on $MONGODB_HOST..." \n\
for i in {1..30}; do \n\
  if nc -z -w 3 $MONGODB_HOST 27017; then \n\
    echo "✅ Successfully connected to MongoDB" \n\
    # Try MongoDB authentication \n\
    echo "🔐 Testing MongoDB authentication..." \n\
    if command -v mongosh >/dev/null 2>&1; then \n\
      if mongosh --quiet --eval "db.runCommand({ping:1}).ok" admin --host $MONGODB_HOST --port 27017 -u $MONGODB_USERNAME -p $MONGODB_PASSWORD --authenticationDatabase admin | grep -q "1"; then \n\
        echo "✅ MongoDB authentication successful" \n\
      else \n\
        echo "⚠️ MongoDB authentication failed, but will continue startup" \n\
      fi \n\
    else \n\
      echo "ℹ️ mongosh command not available, skipping authentication test" \n\
    fi \n\
    break \n\
  fi \n\
  echo "⏳ Waiting for MongoDB to be available... ($i/30)" \n\
  if [ $i -eq 30 ]; then \n\
    echo "⚠️ Could not connect to MongoDB within timeout, but will continue startup" \n\
  fi \n\
  sleep 2 \n\
done \n\
\n\
# Extract Kafka host from bootstrap servers \n\
KAFKA_BOOTSTRAP_SERVERS=${KAFKA_BOOTSTRAP_SERVERS:-kafka:9092} \n\
KAFKA_HOST=$(echo $KAFKA_BOOTSTRAP_SERVERS | cut -d: -f1) \n\
KAFKA_PORT=$(echo $KAFKA_BOOTSTRAP_SERVERS | cut -d: -f2) \n\
\n\
# Check Kafka connection with more retries \n\
echo "🔍 Testing connection to Kafka on $KAFKA_HOST:$KAFKA_PORT..." \n\
for i in {1..20}; do \n\
  if nc -z -w 3 $KAFKA_HOST $KAFKA_PORT; then \n\
    echo "✅ Successfully connected to Kafka" \n\
    break \n\
  fi \n\
  echo "⏳ Waiting for Kafka to be available... ($i/20)" \n\
  if [ $i -eq 20]; then \n\
    echo "⚠️ Could not connect to Kafka within timeout, will continue but Kafka features may not work" \n\
  fi \n\
  sleep 2 \n\
done \n\
\n\
# Extract Redis host information \n\
REDIS_HOST=${REDIS_HOST:-redis} \n\
REDIS_PORT=${REDIS_PORT:-6379} \n\
\n\
# Check Redis connection \n\
echo "🔍 Testing connection to Redis on $REDIS_HOST:$REDIS_PORT..." \n\
for i in {1..15}; do \n\
  if nc -z -w 3 $REDIS_HOST $REDIS_PORT; then \n\
    echo "✅ Successfully connected to Redis" \n\
    break \n\
  fi \n\
  echo "⏳ Waiting for Redis to be available... ($i/15)" \n\
  if [ $i -eq 15]; then \n\
    echo "⚠️ Could not connect to Redis within timeout, but will continue startup" \n\
  fi \n\
  sleep 2 \n\
done \n\
\n\
# Extract Eureka host information \n\
EUREKA_HOST=${EUREKA_HOST:-eureka-server} \n\
EUREKA_PORT=${EUREKA_PORT:-8761} \n\
\n\
# Check Eureka connection \n\
echo "🔍 Testing connection to Eureka on $EUREKA_HOST:$EUREKA_PORT..." \n\
for i in {1..15}; do \n\
  if nc -z -w 3 $EUREKA_HOST $EUREKA_PORT; then \n\
    echo "✅ Successfully connected to Eureka" \n\
    break \n\
  fi \n\
  echo "⏳ Waiting for Eureka to be available... ($i/15)" \n\
  if [ $i -eq 15]; then \n\
    echo "⚠️ Could not connect to Eureka within timeout, but will continue startup" \n\
  fi \n\
  sleep 2 \n\
done \n\
\n\
# Print container network information for debugging \n\
echo "📡 Network Information:" \n\
ip addr show || echo "Cannot show network interfaces" \n\
echo "Network routes:" \n\
ip route || echo "Cannot show routes" \n\
\n\
# Log all relevant environment variables for debugging \n\
echo "📋 Environment Configuration:" \n\
echo "SPRING_DATA_MONGODB_URI: [MASKED]" \n\
echo "KAFKA_BOOTSTRAP_SERVERS: $KAFKA_BOOTSTRAP_SERVERS" \n\
echo "REDIS_HOST: $REDIS_HOST" \n\
echo "EUREKA_HOST: $EUREKA_HOST" \n\
\n\
echo "🚀 Starting application with options: $JAVA_OPTS" \n\
exec java $JAVA_OPTS -jar app.jar \n\
' > /startup.sh
RUN chmod +x /startup.sh

# Create healthcheck script with improved diagnostics
RUN echo '#!/bin/sh \n\
# First try standard health endpoint \n\
if curl -f http://localhost:8056/actuator/health > /tmp/health.json 2>/dev/null; then \n\
  echo "Health check passed via actuator" \n\
  # Check MongoDB component status \n\
  MONGO_STATUS=$(cat /tmp/health.json | jq -r ".components.mongo.status" 2>/dev/null) \n\
  if [ "$MONGO_STATUS" = "UP" ]; then \n\
    echo "MongoDB connection is healthy" \n\
  else \n\
    echo "Warning: MongoDB component status: $MONGO_STATUS" \n\
  fi \n\
  exit 0 \n\
else \n\
  echo "Health check failed" \n\
  # Try to get more diagnostic information \n\
  curl -s http://localhost:8056/actuator/health 2>/dev/null || echo "Cannot connect to health endpoint" \n\
  # Check if MongoDB is reachable \n\
  if [[ "$SPRING_DATA_MONGODB_URI" =~ @([^:/]+) ]]; then \n\
    MONGODB_HOST="${BASH_REMATCH[1]}" \n\
    if nc -z -w 3 $MONGODB_HOST 27017; then \n\
      echo "MongoDB is reachable, but application health check still failed" \n\
    else \n\
      echo "Cannot connect to MongoDB at $MONGODB_HOST" \n\
    fi \n\
  fi \n\
  exit 1 \n\
fi \n\
' > /healthcheck.sh
RUN chmod +x /healthcheck.sh

# Set environment variables
ENV SPRING_PROFILES_ACTIVE=prod \
    KAFKA_ENABLED=true \
    SPRING_KAFKA_ADMIN_FAIL_FAST=false \
    SPRING_CLOUD_CONFIG_FAIL_FAST=false \
    JAVA_TOOL_OPTIONS="\
    -XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:+UseG1GC \
    -Dio.netty.tryReflectionSetAccessible=true \
    -Djava.security.egd=file:/dev/./urandom \
    -Djava.awt.headless=true \
    -Dfile.encoding=UTF-8"

# Health check configuration with longer initial delay
HEALTHCHECK --interval=30s --timeout=10s --start-period=120s --retries=3 \
  CMD /healthcheck.sh

EXPOSE 8056
ENTRYPOINT ["/startup.sh"]