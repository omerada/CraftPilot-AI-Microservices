# Node.js ve Lighthouse kurulumu için temel imaj
FROM node:16-slim AS node-base

# Mimariye göre Chrome kurulumu için değişkenler ayarla
ARG TARGETARCH
ARG CHROME_ARCH=${TARGETARCH:-amd64}

# Gerekli paketleri kur ve Lighthouse'u global olarak yükle
RUN apt-get update \
    && apt-get install -y wget gnupg ca-certificates procps libxss1 apt-transport-https \
    && wget -q -O - https://dl-ssl.google.com/linux/linux_signing_key.pub | apt-key add - \
    # ARM64 mimarisi için Chromium kullan, AMD64 için Google Chrome
    && if [ "$CHROME_ARCH" = "arm64" ]; then \
          apt-get install -y chromium; \
       else \
          sh -c 'echo "deb [arch=amd64] http://dl.google.com/linux/chrome/deb/ stable main" >> /etc/apt/sources.list.d/google.list' \
          && apt-get update \
          && apt-get install -y google-chrome-stable; \
       fi \
    && npm install -g lighthouse@9.6.8 puppeteer@13.5.1 \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

# Chrome binary path'i ayarla
RUN if [ "$CHROME_ARCH" = "arm64" ]; then \
      echo '#!/bin/bash\nchromium "$@"' > /usr/local/bin/google-chrome \
      && chmod +x /usr/local/bin/google-chrome; \
    fi

# Java uygulaması için temel imaj
FROM eclipse-temurin:17-jdk AS builder

WORKDIR /app
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY src src

# Maven ile uygulamayı derle
RUN ./mvnw package -DskipTests

# Nihai imaj
FROM eclipse-temurin:17-jre

# Node.js, Chrome ve Lighthouse'u kopyala
COPY --from=node-base /usr/local/bin/node /usr/local/bin/
COPY --from=node-base /usr/local/bin/npm /usr/local/bin/
COPY --from=node-base /usr/local/bin/lighthouse /usr/local/bin/
COPY --from=node-base /usr/local/lib/node_modules /usr/local/lib/node_modules
COPY --from=node-base /usr/local/bin/google-chrome /usr/local/bin/

# Chrome binarylerini kopyalamak için shell komutlar içeren bir script oluştur
RUN echo '#!/bin/bash \n\
    if [ -d "/usr/bin/chromium" ]; then \n\
        cp -r /usr/bin/chromium* /usr/bin/ 2>/dev/null || true \n\
    fi \n\
    if [ -d "/usr/bin/google-chrome" ]; then \n\
        cp -r /usr/bin/google-chrome* /usr/bin/ 2>/dev/null || true \n\
    fi' > /tmp/copy-chrome.sh \
    && chmod +x /tmp/copy-chrome.sh

# Diğer yoldan chrome binary kopyalamayı dene
COPY --from=node-base /usr/bin/chromium /usr/bin/chromium || true
COPY --from=node-base /usr/bin/google-chrome /usr/bin/google-chrome || true

WORKDIR /app

# Java uygulamasını kopyala
COPY --from=builder /app/target/*.jar app.jar

# Çalışma dizinini ve kullanıcıyı oluştur
RUN mkdir -p /tmp/lighthouse && \
    useradd -m -d /home/appuser -s /bin/bash appuser && \
    chown -R appuser:appuser /app /tmp/lighthouse

# Gerekli izinleri ayarla
RUN chmod 755 /usr/local/bin/google-chrome && \
    chmod -R 777 /tmp/lighthouse

# Kullanıcıyı değiştir
USER appuser

# Uygulama portunu belirle
EXPOSE 8085

# Uygulamayı başlat
ENTRYPOINT ["java", "-jar", "app.jar"]
