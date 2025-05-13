# Build stage
FROM maven:3.9.6-eclipse-temurin-21 AS build

WORKDIR /app

# Sadece pom.xml dosyasını kopyala ve bağımlılıkları indir
COPY pom.xml .
# Maven bağımlılık indirme hatası olsa bile devam et
RUN mvn dependency:go-offline || echo "Dependencies may not be completely downloaded"

# Kaynak kodları kopyala ve paketi oluştur
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

# Create startup script for better initialization
COPY <<EOF /app/startup.sh
#!/bin/bash
set -e

echo "Starting Analytics Service..."
echo "Java version: $(java -version 2>&1 | head -n 1)"
echo "Environment checks:"

# MongoDB host ve kimlik bilgilerini belirle
# SPRING_DATA_MONGODB_URI'dan host ve kimlik bilgilerini çıkarma (geliştirilmiş regex)
if [[ -n "\$SPRING_DATA_MONGODB_URI" && "\$SPRING_DATA_MONGODB_URI" =~ mongodb://([^:]*):([^@]*)@([^:/]+) ]]; then
  MONGODB_USERNAME="\${BASH_REMATCH[1]}"
  MONGODB_PASSWORD="\${BASH_REMATCH[2]}"
  MONGODB_HOST="\${BASH_REMATCH[3]}"
  echo "📊 MongoDB URI'dan kimlik bilgileri çıkarıldı - Kullanıcı: \$MONGODB_USERNAME, Host: \$MONGODB_HOST"
else
  # Değişkenler tanımlı değilse varsayılan değerleri kullan
  MONGODB_USERNAME="\${MONGO_ROOT_USERNAME:-craftpilot}"
  MONGODB_PASSWORD="\${MONGO_ROOT_PASSWORD:-secure_password}"
  MONGODB_HOST="\${MONGODB_HOST:-craftpilot-mongodb}"
  echo "⚠️ Varsayılan MongoDB bilgileri kullanılıyor - Kullanıcı: \$MONGODB_USERNAME, Host: \$MONGODB_HOST"
fi

echo "Testing connection to MongoDB at \$MONGODB_HOST..."
for i in {1..30}; do
  if nc -z -w 2 \$MONGODB_HOST 27017; then
    echo "✅ MongoDB'ye başarıyla bağlanıldı: \$MONGODB_HOST:27017"
    
    # Kimlik doğrulama kontrolü (eğer mongosh varsa)
    if command -v mongosh >/dev/null 2>&1; then
      echo "MongoDB kimlik doğrulama testi yapılıyor..."
      if mongosh --quiet --eval "db.runCommand({ping:1}).ok" admin --host \$MONGODB_HOST --port 27017 -u \$MONGODB_USERNAME -p \$MONGODB_PASSWORD --authenticationDatabase admin | grep -q "1"; then
        echo "✅ MongoDB kimlik doğrulama başarılı"
      else
        echo "⚠️ MongoDB kimlik doğrulama başarısız, ancak bağlantı mevcut"
      fi
    fi
    break
  fi
  echo "⏳ MongoDB'nin hazır olması bekleniyor... (\$i/30)"
  if [ \$i -eq 30 ]; then
    echo "⚠️ MongoDB bağlantı zaman aşımı, servis başlatılacak ancak MongoDB bağlantısı olmayabilir!"
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
    if [ \$i -eq 15]; then
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
    if [ \$i -eq 10]; then
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
HEALTH_ENDPOINT="http://localhost:\${SERVER_PORT:-8064}/actuator/health"
echo "Checking health at: \$HEALTH_ENDPOINT"

# Try with curl
if curl -f \$HEALTH_ENDPOINT > /dev/null 2>&1; then
  echo "Health check passed"
  exit 0
else
  echo "Health check failed"
  # More diagnostic information
  echo "Service logs:"
  tail -n 20 /tmp/analytics-service.log 2>/dev/null || echo "No logs available"
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