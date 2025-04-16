# Lighthouse başlangıç imajı
FROM node:18-slim AS node-base

# Gerekli sistem paketlerini yükle
RUN apt-get update && apt-get install -y \
    chromium \
    fonts-ipafont-gothic \
    fonts-wqy-zenhei \
    fonts-thai-tlwg \
    fonts-kacst \
    fonts-symbola \
    fonts-noto \
    fonts-noto-color-emoji \
    fonts-freefont-ttf \
    ca-certificates \
    curl \
    gnupg \
    --no-install-recommends \
    && rm -rf /var/lib/apt/lists/*

# Lighthouse ve gerekli modülleri global olarak kur
RUN npm install -g lighthouse@11.1.0 \
                    chrome-launcher@0.15.2 \
                    axe-core@4.7.0 \
                    puppeteer@21.3.8 \
                    && npm cache clean --force

# Lighthouse çalıştırılabilir olduğunu doğrula
RUN ln -s /usr/bin/chromium /usr/bin/chromium-browser \
    && ln -s /usr/bin/chromium /usr/bin/google-chrome \
    && mkdir -p /root/.config \
    && chmod -R 777 /root/.config \
    && lighthouse --version

# Java uygulaması için builder aşaması
FROM eclipse-temurin:21-jdk AS builder
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

# Node.js ve Lighthouse CLI kurulumu için gerekli bağımlılıklar
RUN apt-get update && apt-get install -y --no-install-recommends \
    chromium \
    fonts-ipafont-gothic \
    fonts-wqy-zenhei \
    fonts-thai-tlwg \
    fonts-kacst \
    fonts-symbola \
    fonts-noto \
    fonts-noto-color-emoji \
    fonts-freefont-ttf \
    ca-certificates \
    curl \
    wget \
    gnupg \
    dumb-init \
    procps \
    netcat-traditional \
    && rm -rf /var/lib/apt/lists/*

# Node.js kurulumu
COPY --from=node-base /usr/local/bin/node /usr/local/bin/
COPY --from=node-base /usr/local/bin/npm /usr/local/bin/

# Lighthouse CLI ve bağımlılıkları
COPY --from=node-base /usr/local/lib/node_modules /usr/local/lib/node_modules
COPY --from=node-base /usr/bin/chromium /usr/bin/chromium
COPY --from=node-base /usr/bin/google-chrome /usr/bin/google-chrome
COPY --from=node-base /usr/bin/chromium-browser /usr/bin/chromium-browser

# Lighthouse CLI sembolik link oluştur
RUN ln -s /usr/local/lib/node_modules/lighthouse/cli/index.js /usr/local/bin/lighthouse \
    && chmod +x /usr/local/bin/lighthouse

# Lighthouse ve Chrome versiyonlarını kontrol et
RUN lighthouse --version && chromium --version

# Java uygulaması
COPY --from=builder /app/target/*.jar /app/app.jar

# Geçici klasör ve kullanıcı ayarları
RUN mkdir -p /tmp/lighthouse /tmp/node_modules /home/appuser/.config \
    && useradd -m -d /home/appuser -s /bin/bash appuser \
    && chown -R appuser:appuser /app /tmp/lighthouse /tmp/node_modules /home/appuser/.config \
    && chmod -R 777 /tmp/lighthouse /tmp/node_modules /home/appuser/.config

# Çalışma sırasında kullanılacak kullanıcı
USER appuser

# Çalışma ortamı değişkenleri
ENV HOME=/home/appuser \
    NODE_PATH=/usr/local/lib/node_modules \
    CHROME_PATH=/usr/bin/chromium \
    LIGHTHOUSE_CHROMIUM_PATH=/usr/bin/chromium \
    PUPPETEER_SKIP_CHROMIUM_DOWNLOAD=true \
    PUPPETEER_EXECUTABLE_PATH=/usr/bin/chromium \
    LIGHTHOUSE_CI_CACHE_PATH=/tmp/lighthouse

WORKDIR /app
EXPOSE 8085

# dumb-init ile process yönetimi
ENTRYPOINT ["/usr/bin/dumb-init", "--"]
CMD ["java", "-jar", "app.jar"]
