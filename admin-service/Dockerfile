# Build stage
FROM maven:3.9.6-eclipse-temurin-21 AS build

WORKDIR /build

# Cache Maven dependencies
COPY pom.xml ./
RUN mvn dependency:go-offline

# Build the application
COPY src ./src/
RUN mvn clean package -DskipTests

# Run stage
FROM eclipse-temurin:21-jre

WORKDIR /app

# Copy application JAR
COPY --from=build /build/target/*.jar app.jar

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

# Create startup script
COPY <<EOF /app/startup.sh
#!/bin/bash
set -e

echo "Starting Admin Service..."
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

# Check Redis connection
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

echo "Starting application with options: \$JAVA_OPTS"
exec java \$JAVA_OPTS -jar app.jar
EOF

RUN chmod +x /app/startup.sh

# Configure health check
HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=3 \
  CMD curl -f http://localhost:8063/actuator/health || exit 1

EXPOSE 8063
EXPOSE 5005

ENTRYPOINT ["/app/startup.sh"]