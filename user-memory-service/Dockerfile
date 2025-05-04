# 1. Build aşaması: Maven ile derleme
FROM eclipse-temurin:21-jdk-alpine as builder
WORKDIR /app

# Maven kurulumu
RUN apk update && apk add maven

# Bağımlılıkları önce kopyalayıp, güncel değilse tekrar yüklememesi için
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Kaynak kodunu kopyalayıp build et
COPY src src
RUN mvn package -DskipTests

# 2. Çalışma aşaması: Sadece JRE ile çalıştırma
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar

# Güvenlik için root olmayan kullanıcı oluştur
RUN addgroup -S craftpilot && adduser -S appuser -G craftpilot
USER appuser

# Healthcheck
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD wget -q --spider http://localhost:8067/actuator/health/liveness || exit 1

# JVM ayarları optimize edildi
ENV JAVA_OPTS="-XX:+UseG1GC -XX:MaxGCPauseMillis=100 -XX:+ParallelRefProcEnabled -Xms512m -Xmx1g -Dfile.encoding=UTF-8"

# Actuator port
EXPOSE 8067

# Daha iyi container sinyalleri için
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
