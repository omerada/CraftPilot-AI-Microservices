# Chrome ve Lighthouse kurulumu için temel imaj - daha güncel Node sürümü
FROM node:18-slim AS node-base

ARG TARGETARCH
ARG CHROME_ARCH=${TARGETARCH:-amd64}

# Gerekli sistem paketlerini yükle ve Lighthouse kurulumu
RUN apt-get update && apt-get install -y \
    chromium \
    libxss1 \
    ca-certificates \
    wget \
    gnupg \
    apt-transport-https \
    procps \
    netcat-traditional \
    curl \
    dumb-init \
    # Ek Chrome bağımlılıkları
    libnss3 \
    libatk1.0-0 \
    libatk-bridge2.0-0 \
    libcups2 \
    libdrm2 \
    libxkbcommon0 \
    libxcomposite1 \
    libxdamage1 \
    libxfixes3 \
    libxrandr2 \
    && npm install -g lighthouse@10.0.0 puppeteer@19.7.0 \
    # Lighthouse çalışması için gerekli global modülleri ekle
    && npm install -g chrome-launcher \
    && npm install -g axe-core \
    && npm cache clean --force \
    && apt-get clean && rm -rf /var/lib/apt/lists/*

# Chromium alias'ını oluştur (Lighthouse'un kullanabilmesi için)
RUN ln -s /usr/bin/chromium /usr/bin/google-chrome && \
    # Lighthouse'un çalışabilmesi için gerekli dizinleri oluştur
    mkdir -p /home/node/.cache/puppeteer && \
    mkdir -p /home/node/.config/configstore && \
    chmod -R 777 /home/node

# Chrome versiyonunu kontrol et
RUN chromium --version || echo "Chrome not working properly"

# Lighthouse CLI'ı test et
RUN lighthouse --version || echo "Lighthouse CLI not working properly"

# Java uygulaması için builder aşaması
FROM eclipse-temurin:17-jdk AS builder
WORKDIR /app

# Maven wrapper ve bağımlılıkları önce kopyalayıp önbelleği etkin kullan
COPY mvnw ./ 
COPY .mvn .mvn
COPY pom.xml ./
# Bağımlılıkları önce indirerek build katmanını optimize et
RUN ./mvnw dependency:go-offline

# Kaynak kodu kopyala ve derle
COPY src ./src
RUN ./mvnw clean package -DskipTests

# Final imaj (sadece çalışma için gerekli her şey)
FROM eclipse-temurin:21-jre

# Node ve Lighthouse CLI
COPY --from=node-base /usr/local/bin/node /usr/local/bin/
COPY --from=node-base /usr/local/bin/npm /usr/local/bin/
COPY --from=node-base /usr/local/bin/npx /usr/local/bin/
COPY --from=node-base /usr/local/bin/lighthouse /usr/local/bin/
COPY --from=node-base /usr/local/lib/node_modules /usr/local/lib/node_modules
COPY --from=node-base /usr/bin/chromium /usr/bin/chromium
COPY --from=node-base /usr/bin/google-chrome /usr/bin/google-chrome
COPY --from=node-base /usr/bin/dumb-init /usr/bin/dumb-init

# Chrome için gerekli sistem paketlerini ekle
RUN apt-get update && apt-get install -y --no-install-recommends \
    procps \
    curl \
    netcat-traditional \
    libxss1 \
    # Chrome için gerekli ek kütüphaneler
    libnss3 \
    libatk1.0-0 \
    libatk-bridge2.0-0 \
    libcups2 \
    libdrm2 \
    libxkbcommon0 \
    libxcomposite1 \
    libxdamage1 \
    libxfixes3 \
    libxrandr2 \
    && apt-get clean && rm -rf /var/lib/apt/lists/*

# Gerekli NodeJS modüllerinin sembolik linklerini oluştur
RUN ln -s /usr/local/lib/node_modules/lighthouse/cli/index.js /usr/local/bin/lighthouse && \
    chmod +x /usr/local/bin/lighthouse

# Java uygulaması
COPY --from=builder /app/target/*.jar /app/app.jar

# Geçici klasör, kullanıcı ayarları ve Chrome için dizinleri oluştur
RUN mkdir -p /tmp/lighthouse && \
    mkdir -p /tmp/.X11-unix && \
    mkdir -p /tmp/.lighthouse && \
    mkdir -p /var/cache/chromium && \
    useradd -m -d /home/appuser -s /bin/bash appuser && \
    # Chrome ve Lighthouse için gerekli dizinleri oluştur
    mkdir -p /home/appuser/.cache/puppeteer && \
    mkdir -p /home/appuser/.config/configstore && \
    # Tüm izinleri ayarla
    chown -R appuser:appuser /app /tmp/lighthouse /tmp/.X11-unix /tmp/.lighthouse /var/cache/chromium && \
    chown -R appuser:appuser /home/appuser/.cache /home/appuser/.config && \
    chmod -R 777 /tmp/lighthouse /tmp/.X11-unix /tmp/.lighthouse /var/cache/chromium /home/appuser/.cache /home/appuser/.config

# Chrome ve Lighthouse CLI'ın düzgün çalıştığını kontrol et
USER appuser
RUN chromium --version || echo "Chrome check failed" && \
    lighthouse --version || echo "Lighthouse check failed" && \
    node -e "console.log('Node.js ' + process.version)"

# Çalışma dizini ve port ayarları
WORKDIR /app
EXPOSE 8085

# Çevre değişkenlerini ayarla
ENV HOME=/home/appuser \
    CHROME_PATH=/usr/bin/chromium \
    PUPPETEER_SKIP_CHROMIUM_DOWNLOAD=true \
    PUPPETEER_EXECUTABLE_PATH=/usr/bin/chromium \
    NODE_PATH=/usr/local/lib/node_modules

# dumb-init ile process yönetimini iyileştir
ENTRYPOINT ["/usr/bin/dumb-init", "--"]
CMD ["java", "-jar", "app.jar"]
