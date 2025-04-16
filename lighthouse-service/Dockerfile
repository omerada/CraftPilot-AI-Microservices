# Build stage - Maven ve Java kullanarak derleme
FROM maven:3.9.5-eclipse-temurin-17 AS builder
WORKDIR /app

# POM dosyasını kopyala
COPY pom.xml .

# Bağımlılıkları indir (pom.xml değişmediği sürece önbelleğe alınır)
RUN mvn dependency:go-offline -B

# Kaynak kodları kopyala
COPY src ./src

# Uygulamayı derle
RUN mvn clean package -DskipTests -B

# Final imaj - Debian tabanlı imaj
FROM eclipse-temurin:17-jre

# Temel araçları kur
RUN apt-get update && apt-get install -y \
    nodejs \
    npm \
    curl \
    wget \
    gnupg \
    procps \
    lsof \
    chromium \
    --no-install-recommends && \
    # Node.js ve NPM sürümlerini kontrol et
    node --version && \
    npm --version && \
    # Lighthouse CLI kurulumu
    npm install -g lighthouse@11.1.0 && \
    npm cache clean --force && \
    # Lighthouse CLI'yı doğrula
    lighthouse --version

# Gerekli dizinleri oluştur
RUN mkdir -p /tmp/lighthouse /home/appuser /tmp/netty

# Ortam değişkenlerini yapılandır
ENV NODE_PATH=/usr/local/lib/node_modules \
    CHROME_PATH=/usr/bin/chromium \
    LIGHTHOUSE_CHROMIUM_PATH=/usr/bin/chromium \
    PUPPETEER_SKIP_CHROMIUM_DOWNLOAD=true \
    JAVA_HOME=/opt/java/openjdk \
    PATH="${JAVA_HOME}/bin:/usr/local/bin:/usr/bin:${PATH}" \
    SPRING_PROFILES_ACTIVE=prod

# Güvenlik için non-root kullanıcı oluştur
RUN groupadd -r appuser && \
    useradd -r -g appuser -d /home/appuser appuser && \
    chown -R appuser:appuser /tmp/lighthouse /tmp/netty /home/appuser

# JAR dosyasını kopyala
COPY --from=builder /app/target/*.jar /app/app.jar
RUN chown appuser:appuser /app/app.jar

# Healthcheck script ekle
COPY healthcheck.sh /app/healthcheck.sh
RUN chmod +x /app/healthcheck.sh && \
    chown appuser:appuser /app/healthcheck.sh

# Kurulumu doğrula
RUN java -version && \
    node --version && \
    npm --version && \
    chromium --version || echo "Chromium version check failed" && \
    lighthouse --version && \
    ls -la /app/app.jar && \
    which java && \
    which node && \
    which npm && \
    which lighthouse

# Non-root kullanıcıya geç
USER appuser
WORKDIR /app
EXPOSE 8085

# Healthcheck yapılandır
HEALTHCHECK --interval=30s --timeout=20s --start-period=90s --retries=3 CMD [ "bash", "/app/healthcheck.sh" ]

# Uygulamayı çalıştır
CMD ["java", "-jar", "app.jar"]
