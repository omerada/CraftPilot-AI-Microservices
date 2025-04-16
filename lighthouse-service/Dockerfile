# Build stage - Maven ve Java kullanarak derleme
FROM maven:3.9.5-eclipse-temurin-21 AS builder
WORKDIR /app

# POM dosyasını kopyala
COPY pom.xml .

# Bağımlılıkları indir (pom.xml değişmediği sürece önbelleğe alınır)
RUN mvn dependency:go-offline -B

# Kaynak kodları kopyala
COPY src ./src

# Uygulamayı derle
RUN mvn clean package -DskipTests -B

# Final imaj - Alpine yerine tam Debian tabanlı imaj kullan
FROM eclipse-temurin:21-jre

# Temel araçları kur
RUN apt-get update && apt-get install -y \
    nodejs \
    npm \
    curl \
    chromium \
    wget \
    gnupg \
    procps \
    lsof \
    --no-install-recommends && \
    rm -rf /var/lib/apt/lists/*

# Java yolunun düzgün ayarlanması
ENV JAVA_HOME=/opt/java/openjdk
ENV PATH="${JAVA_HOME}/bin:${PATH}"

# Lighthouse CLI kurulumu
RUN npm install -g lighthouse@11.1.0 && \
    npm cache clean --force

# Gerekli dizinleri oluştur
RUN mkdir -p /tmp/lighthouse /home/appuser /tmp/netty

# Ortam değişkenlerini yapılandır
ENV NODE_PATH=/usr/local/lib/node_modules \
    CHROME_PATH=/usr/bin/chromium \
    LIGHTHOUSE_CHROMIUM_PATH=/usr/bin/chromium \
    PUPPETEER_SKIP_CHROMIUM_DOWNLOAD=true \
    PATH="${PATH}:/usr/local/bin:/usr/bin"

# Güvenlik için non-root kullanıcı oluştur
RUN groupadd -r appuser && \
    useradd -r -g appuser -d /home/appuser appuser && \
    chown -R appuser:appuser /tmp/lighthouse /tmp/netty /home/appuser

# JAR dosyasını kopyala
COPY --from=builder /app/target/*.jar /app/app.jar
RUN chown appuser:appuser /app/app.jar

# Kurulumu doğrula
RUN java -version && \
    node --version && \
    npm --version && \
    echo "Checking Chrome:" && \
    chromium --version || echo "Chromium version check failed" && \
    echo "Checking Lighthouse:" && \
    lighthouse --version && \
    echo "Checking JAR file:" && \
    ls -la /app/app.jar && \
    echo "Checking Java PATH:" && \
    which java

# Healthcheck için basic script ekle
COPY healthcheck.sh /app/healthcheck.sh
RUN chmod +x /app/healthcheck.sh

# Non-root kullanıcıya geç
USER appuser
WORKDIR /app
EXPOSE 8085

# Healthcheck yapılandır
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 CMD [ "bash", "/app/healthcheck.sh" ]

# Uygulamayı çalıştır
CMD ["java", "-jar", "app.jar"]
