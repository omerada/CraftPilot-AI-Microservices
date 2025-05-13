# Build stage
FROM maven:3.9.6-eclipse-temurin-21 AS build

WORKDIR /build

# Proje dosyalarını kopyala
COPY pom.xml ./
COPY src ./src/

# Maven ile build
RUN mvn clean package -DskipTests

# Run stage
FROM eclipse-temurin:21-jre

WORKDIR /app

# Uygulamayı kopyala
COPY --from=build /build/target/*.jar app.jar

# Gerekli araçları kur
RUN apt-get update && \
    apt-get install -y --no-install-recommends curl procps net-tools jq && \
    rm -rf /var/lib/apt/lists/*

# Sağlık kontrolü
HEALTHCHECK --interval=30s --timeout=15s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8063/actuator/health || exit 1

# Port aç
EXPOSE 8063
# Debug portu - isteğe bağlı
EXPOSE 5005

# Başlatma scriptini kopyala
COPY <<EOF /app/startup.sh
#!/bin/bash
echo "Starting Admin Service..."
echo "Java version:"
java -version
echo "Environment:"
env | grep -E 'SPRING|KAFKA|MONGO|REDIS|EUREKA'
echo "Starting application with options: \$JAVA_OPTS"

# Bağımlı servislerin hazır olmasını bekle
echo "Checking dependencies before starting..."

# MongoDB bağlantısını kontrol et
if [[ "\$SPRING_DATA_MONGODB_URI" == *"@"* ]]; then
  MONGODB_HOST=\$(echo \$SPRING_DATA_MONGODB_URI | sed -n 's/.*@\\([^:]*\\).*/\\1/p')
else
  MONGODB_HOST=\$(echo \$SPRING_DATA_MONGODB_URI | sed -n 's/.*\\/\\/\\([^:]*\\).*/\\1/p')
fi

echo "Waiting for MongoDB at \$MONGODB_HOST..."
for i in {1..30}; do
  if ping -c 1 \$MONGODB_HOST &> /dev/null; then
    echo "MongoDB server reachable"
    break
  fi
  echo "Waiting for MongoDB to be reachable (\$i/30)..."
  sleep 2
done

# Redis bağlantısını kontrol et
echo "Waiting for Redis at \$REDIS_HOST..."
for i in {1..15}; do
  if ping -c 1 \$REDIS_HOST &> /dev/null; then
    echo "Redis server reachable"
    break
  fi
  echo "Waiting for Redis to be reachable (\$i/15)..."
  sleep 2
done

echo "Dependency checks completed, starting application..."
exec java \$JAVA_OPTS -jar app.jar
EOF

RUN chmod +x /app/startup.sh

ENTRYPOINT ["/app/startup.sh"]