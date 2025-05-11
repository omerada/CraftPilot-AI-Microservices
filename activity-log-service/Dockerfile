# Build stage
FROM maven:3.9.6-eclipse-temurin-17 AS build

# CI/CD ortamında doğru dizin yapısını kullan
WORKDIR /build

# Önce sadece proje dosyalarını kopyala
COPY activity-log-service/pom.xml ./pom.xml
COPY activity-log-service/src ./src

# Eğer craft-pilot-commons gerekiyorsa şartlı olarak kopyala
COPY craft-pilot-commons /build-lib/craft-pilot-commons
RUN if [ -d "/build-lib/craft-pilot-commons" ]; then \
    cd /build-lib/craft-pilot-commons && mvn clean install -DskipTests; \
    fi

# Maven build
RUN mvn clean package -DskipTests

# Run stage
FROM eclipse-temurin:17-jre

WORKDIR /app
COPY --from=build /build/target/*.jar app.jar

# JVM settings
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=70.0 -XX:+UseG1GC -Djava.security.egd=file:/dev/./urandom"

# Install curl for healthcheck
RUN apt-get update && \
    apt-get install -y --no-install-recommends curl && \
    rm -rf /var/lib/apt/lists/*

# Simple healthcheck configuration
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8095/actuator/health || exit 1

EXPOSE 8095

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
