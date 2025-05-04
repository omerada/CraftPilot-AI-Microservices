FROM eclipse-temurin:21-jdk-alpine as builder
WORKDIR /app
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY src src
RUN ./mvnw package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar

# Güvenlik için root olmayan kullanıcı oluştur
RUN addgroup -S craftpilot && adduser -S appuser -G craftpilot
USER appuser

# Healthcheck
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD wget -q --spider http://localhost:8067/actuator/health/liveness || exit 1

# JVM ayarları optimize edildi
ENV JAVA_OPTS="-XX:+UseG1GC -XX:MaxGCPauseMillis=100 -XX:+ParallelRefProcEnabled -Xms512m -Xmx1g -Dfile.encoding=UTF-8"

# Actuator port
EXPOSE 8067

# Daha iyi container sinyalleri için
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
