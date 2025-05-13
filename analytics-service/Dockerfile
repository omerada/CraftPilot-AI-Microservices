# Build stage
FROM maven:3.9.6-eclipse-temurin-21 AS build

WORKDIR /app

# Cache Maven dependencies
COPY pom.xml ./
RUN mvn dependency:go-offline

# Build the application
COPY src ./src/
RUN mvn clean package -DskipTests

# Run stage
FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

# Install essential tools for troubleshooting
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
    curl \
    netcat-openbsd \
    iputils-ping \
    dnsutils \
    procps \
    jq && \
    rm -rf /var/lib/apt/lists/*

# MongoDB connection - Default olarak güvenli varsayılan değerler
ENV MONGODB_URI=mongodb://craftpilot:secure_password@mongodb:27017/analytics?authSource=admin&retryWrites=true&w=majority&serverSelectionTimeoutMS=60000
ENV MONGODB_DATABASE=analytics
ENV MONGODB_USERNAME=craftpilot
ENV MONGODB_PASSWORD=secure_password
ENV MONGODB_HOST=mongodb
ENV MONGODB_PORT=27017

# Kafka configuration - Fail-fast olmadan yapılandırma
ENV KAFKA_BOOTSTRAP_SERVERS=kafka:9092
ENV KAFKA_ENABLED=false
ENV KAFKA_ADMIN_AUTO_CREATE=false

# Eureka configuration
ENV EUREKA_SERVER_URL=http://craftpilot:13579ada@eureka-server:8761/eureka/

# Spring and application configs
ENV SPRING_PROFILES_ACTIVE=prod
ENV SERVER_PORT=8064
ENV MONGODB_AUTO_INDEX_CREATION=false
ENV SPRING_DATA_MONGODB_AUTO_INDEX_CREATION=false

# Create startup script for better initialization
COPY <<EOF /app/startup.sh
#!/bin/bash
set -e

echo "Starting Analytics Service..."
echo "Java version: $(java -version 2>&1 | head -n 1)"
echo "Environment checks:"

# Check MongoDB connection
MONGODB_HOST=\$(echo \$SPRING_DATA_MONGODB_URI | sed -n 's/.*@\\([^:]*\\).*/\\1/p')
if [ -z "\$MONGODB_HOST" ]; then
  echo "Warning: Could not extract MongoDB host from URI"
  MONGODB_HOST="craftpilot-mongodb"
fi

echo "Testing connection to MongoDB at \$MONGODB_HOST..."
for i in {1..30}; do
  if nc -z -w 2 \$MONGODB_HOST 27017; then
    echo "✅ Successfully connected to MongoDB at \$MONGODB_HOST:27017"
    break
  fi
  echo "⏳ Waiting for MongoDB to be available... (\$i/30)"
  if [ \$i -eq 30 ]; then
    echo "⚠️ Could not connect to MongoDB within timeout, but will continue startup"
  fi
  sleep 2
done

# Check Redis connection if configured
if [ ! -z "\$REDIS_HOST" ]; then
  echo "Testing connection to Redis at \$REDIS_HOST..."
  for i in {1..15}; do
    if nc -z -w 2 \$REDIS_HOST 6379; then
      echo "✅ Successfully connected to Redis at \$REDIS_HOST:6379"
      break
    fi
    echo "⏳ Waiting for Redis to be available... (\$i/15)"
    if [ \$i -eq 15 ]; then
      echo "⚠️ Could not connect to Redis within timeout, but will continue startup"
    fi
    sleep 2
  done
fi

# Check Kafka connection if enabled
if [ "\$KAFKA_ENABLED" = "true" ] && [ ! -z "\$KAFKA_BOOTSTRAP_SERVERS" ]; then
  KAFKA_HOST=\$(echo \$KAFKA_BOOTSTRAP_SERVERS | cut -d':' -f1)
  KAFKA_PORT=\$(echo \$KAFKA_BOOTSTRAP_SERVERS | cut -d':' -f2)
  
  echo "Testing connection to Kafka at \$KAFKA_HOST:\$KAFKA_PORT..."
  for i in {1..10}; do
    if nc -z -w 2 \$KAFKA_HOST \$KAFKA_PORT; then
      echo "✅ Successfully connected to Kafka at \$KAFKA_HOST:\$KAFKA_PORT"
      break
    fi
    echo "⏳ Waiting for Kafka to be available... (\$i/10)"
    if [ \$i -eq 10 ]; then
      echo "⚠️ Could not connect to Kafka within timeout, but will continue startup"
    fi
    sleep 2
  done
fi

echo "Starting application with options: \$JAVA_OPTS"
exec java \$JAVA_OPTS -jar app.jar
EOF

RUN chmod +x /app/startup.sh

# Create healthcheck script
COPY <<EOF /app/healthcheck.sh
#!/bin/bash
set -e

# Basic health check
HEALTH_ENDPOINT="http://localhost:${SERVER_PORT:-8064}/actuator/health"
echo "Checking health at: $HEALTH_ENDPOINT"

# Try with curl
if curl -f $HEALTH_ENDPOINT > /dev/null 2>&1; then
  echo "Health check passed"
  exit 0
else
  echo "Health check failed"
  exit 1
fi
EOF

RUN chmod +x /app/healthcheck.sh

# Configure health check
HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=3 \
  CMD /app/healthcheck.sh

EXPOSE 8064
EXPOSE 5005

ENTRYPOINT ["/app/startup.sh"]