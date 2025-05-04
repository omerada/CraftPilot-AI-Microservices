# 1. Builder aşaması: Java uygulamasını derle
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

# Gerekli paketleri yükle
RUN apk add --no-cache curl

# Maven yapılandırması
ENV MAVEN_OPTS="-Dmaven.repo.local=./.m2/repository -Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=WARN"

# Bağımlılıkları önce kopyala ve derle (önbellek optimizasyonu için)
COPY pom.xml .
COPY mvnw .
COPY .mvn .mvn
RUN chmod +x ./mvnw

# Maven merkezini ve Google'ın Maven deposunu ekleyerek bağımlılıkları yükle
RUN ./mvnw dependency:go-offline -B \
    -DadditionalRemoteRepositories=https://maven-central.storage.googleapis.com/maven2/ \
    -Dmaven.wagon.http.ssl.insecure=true \
    -Dmaven.wagon.http.ssl.allowall=true

# Kaynak kodları kopyala ve derle
COPY src src
RUN ./mvnw package -DskipTests

# 2. Çalışma aşaması: Sadece JDK ile çalıştırma
FROM eclipse-temurin:21-jdk-alpine AS runtime
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar

# Sağlık kontrolü için curl ekle
RUN apk add --no-cache curl

# GCP credential dosyaları için dizini oluştur
RUN mkdir -p /etc/gcp/credentials

# Firestore/GCP credential dosyası için beklenen ortam değişkenlerini ayarla
ENV GOOGLE_APPLICATION_CREDENTIALS=/etc/gcp/credentials/gcp-credentials.json
ENV SPRING_CLOUD_GCP_CREDENTIALS_LOCATION=file:/etc/gcp/credentials/gcp-credentials.json

# Sistem özelliklerini ve Java opsiyonlarını ayarla
ENV JAVA_TOOL_OPTIONS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

# Healthcheck ekleme
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8067/actuator/health || exit 1

# Uygulama'yı çalıştır
ENTRYPOINT ["java", "-jar", "app.jar"]
