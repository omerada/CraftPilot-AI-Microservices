# Build stage - Official Maven image kullan
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

# Final imaj
FROM eclipse-temurin:21-jre-alpine

# Temel araçları kur
RUN apk add --no-cache \
    nodejs \
    npm \
    chromium \
    curl \
    bash

# Lighthouse CLI kurulumu
RUN npm install -g lighthouse@11.1.0 && \
    npm cache clean --force

# Chromium için sembolik bağlantılar oluştur (güvenli şekilde)
RUN if [ -e /usr/bin/chromium-browser ] && [ ! -e /usr/bin/chromium ]; then \
        ln -sf /usr/bin/chromium-browser /usr/bin/chromium; \
    fi && \
    if [ -e /usr/bin/chromium-browser ] && [ ! -e /usr/bin/google-chrome ]; then \
        ln -sf /usr/bin/chromium-browser /usr/bin/google-chrome; \
    fi

# Gerekli dizinleri oluştur
RUN mkdir -p /tmp/lighthouse /home/appuser

# Ortam değişkenlerini yapılandır
ENV NODE_PATH=/usr/local/lib/node_modules \
    CHROME_PATH=/usr/bin/chromium \
    LIGHTHOUSE_CHROMIUM_PATH=/usr/bin/chromium \
    PUPPETEER_SKIP_CHROMIUM_DOWNLOAD=true \
    PATH="/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:/opt/java/openjdk/bin"

# Güvenlik için non-root kullanıcı oluştur
RUN addgroup -S appuser && \
    adduser -S -G appuser appuser && \
    chown -R appuser:appuser /tmp/lighthouse /home/appuser

# JAR dosyasını kopyala
COPY --from=builder /app/target/*.jar /app/app.jar
RUN chown appuser:appuser /app/app.jar

# Kurulumu doğrula
RUN java -version && \
    node --version && \
    echo "Checking Chrome:" && ls -la /usr/bin/chrom* && \
    echo "Checking Lighthouse:" && which lighthouse && \
    echo "Checking JAR file:" && ls -la /app/app.jar

# Non-root kullanıcıya geç
USER appuser
WORKDIR /app
EXPOSE 8085

# Uygulamayı çalıştır
CMD ["java", "-jar", "app.jar"]
