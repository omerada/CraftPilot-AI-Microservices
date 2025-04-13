# Build için Maven imajı kullann-21-alpine AS redis-lib-builder
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder
# Redis Client Lib'i derle
# Maven build hızlandırma için optimizasyonlar
ENV MAVEN_OPTS="-XX:+TieredCompilation -XX:TieredStopAtLevel=1"s-client-lib/pom.xml .
s-client-lib/src ./src
# Maven bağımlılık önbelleği için volumeRUN mvn clean install -DskipTests
VOLUME /root/.m2
rin-21-alpine AS builder
# Önce sadece POM dosyalarını kopyala (önbellek optimizasyonu için)
WORKDIR /app
COPY pom.xml /app/pp

# Eğer parent POM varsa kopyalan repositorisi redis-client-lib'i içerecek
COPY ../pom.xml /app/parent-pom.xmlCOPY --from=redis-lib-builder /root/.m2/repository /root/.m2/repository
RUN if [ -f "/app/parent-pom.xml" ]; then \offline
        mkdir -p /app/parent && \
        mv /app/parent-pom.xml /app/parent/pom.xml && \
        cd /app/parent && \
        mvn install -DskipTests; \DskipTests
    fi
# Runtime stage
# redis-client-lib'i kontrol et ve varsa derlemurin:21-jre-alpine
COPY ../redis-client-lib/pom.xml /app/redis-client-lib-pom.xml
RUN if [ -f "/app/redis-client-lib-pom.xml" ]; then \builder /app/target/*.jar app.jar
        mkdir -p /app/redis-client-lib && \
        mv /app/redis-client-lib-pom.xml /app/redis-client-lib/pom.xml && \# Healthcheck
        mkdir -p /app/redis-client-lib/src && \-interval=30s --timeout=3s --start-period=60s --retries=3 \
        cd /app/redis-client-lib && \
        mvn install -DskipTests || echo "redis-client-lib build failed, continuing with user-service"; \
    fiEXPOSE 8080
 "app.jar"]# User Service'in bağımlılıklarını indirWORKDIR /app
RUN mvn dependency:go-offline || echo "Some dependencies may not be available, continuing with build"

# Kaynak kodları kopyala
COPY src /app/src/

# Projeyi paketle
RUN mvn package -DskipTests

# Runtime imajı
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar

# Alpine imajına gerekli araçları ekle
RUN apk add --no-cache curl

# Healthcheck
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Port ve çalıştırma komutu
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]