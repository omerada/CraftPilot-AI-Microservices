# 1. Builder aşaması: Java uygulamasını derle
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

# Firestore ve Firebase bağımlılıkları için gerekli paketleri yükle
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
RUN ./mvnw package -DskipTests -X

# 2. Çalışma aşaması: Sadece JDK ile çalıştırma
FROM eclipse-temurin:21-jdk-alpine AS runtime
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar

# Sağlık kontrolü için curl ekle
RUN apk add --no-cache curl

# Firestore/GCP credential dosyası için beklenen ortam değişkenlerini ayarla
ENV GOOGLE_APPLICATION_CREDENTIALS=/etc/gcp/credentials/gcp-credentials.json
ENV FIREBASE_CONFIG_PATH=/etc/gcp/credentials/gcp-credentials.json

# Uygulama'yı çalıştır
ENTRYPOINT ["java", "-jar", "app.jar"]
