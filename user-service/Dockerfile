# Build stage
FROM eclipse-temurin:17-jdk-alpine as build
WORKDIR /build
COPY . .

# Maven için ortam değişkeni ekleme
ARG MAVEN_BUILD_COMMAND="mvn clean package -DskipTests"

# Maven build - daha ayrıntılı hata ayıklama için
RUN ${MAVEN_BUILD_COMMAND}

# Run stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

COPY --from=build /build/target/*.jar app.jar

# JVM settings
ENV JAVA_OPTS="-XX:+UseG1GC -XX:MaxGCPauseMillis=100 -XX:+UseStringDeduplication -Xmx1G -Xms512m"
ENV GOOGLE_APPLICATION_CREDENTIALS=/gcp-credentials.json
ENV SPRING_PROFILES_ACTIVE=prod

# Health check tools - Alpine için apk kullanıyoruz
RUN apk add --no-cache curl

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8060/actuator/health/liveness || exit 1

# Kullanıcı oluşturup izinleri ayarlama
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser:appgroup

EXPOSE 8060
ENTRYPOINT ["java", "-jar", "/app/app.jar"]