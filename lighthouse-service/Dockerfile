# Java builder stage
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app

# Maven wrapper ve bağımlılıkları önce kopyala
COPY mvnw ./
COPY .mvn .mvn
COPY pom.xml ./

# Bağımlılıkları önce indirerek build katmanını optimize et
RUN chmod +x ./mvnw && ./mvnw dependency:go-offline

# Kaynak kodu kopyala ve derle
COPY src ./src
RUN ./mvnw clean package -DskipTests

# Final imaj - daha hafif kurulum
FROM eclipse-temurin:21-jre

# Gerekli bağımlılıkları kur
RUN apt-get update && apt-get install -y --no-install-recommends \
    chromium \
    nodejs \
    npm \
    curl \
    netcat-traditional \
    dumb-init \
    && rm -rf /var/lib/apt/lists/*

# Lighthouse CLI'yi kur
RUN npm install -g lighthouse@11.1.0 && \
    npm cache clean --force

# Chromium için sembolik linkleri koşullu olarak oluştur
RUN which chromium || ln -sf $(which chromium-browser) /usr/bin/chromium || true && \
    which google-chrome || ln -sf $(which chromium-browser) /usr/bin/google-chrome || true && \
    mkdir -p /tmp/lighthouse /home/appuser

# Lighthouse için çevre değişkenleri ayarla
ENV NODE_PATH=/usr/local/lib/node_modules \
    CHROME_PATH=/usr/bin/chromium-browser \
    LIGHTHOUSE_CHROMIUM_PATH=/usr/bin/chromium-browser \
    PUPPETEER_SKIP_CHROMIUM_DOWNLOAD=true

# Gerekli dizinleri oluştur ve uygun izinleri ayarla
RUN useradd -m -d /home/appuser -s /bin/bash appuser && \
    chown -R appuser:appuser /tmp/lighthouse /home/appuser

# Java uygulamasını kopyala
COPY --from=builder /app/target/*.jar /app/app.jar
RUN chown appuser:appuser /app/app.jar

# Kullanıcı değiştir
USER appuser
WORKDIR /app
EXPOSE 8085

CMD ["java", "-jar", "app.jar"]
