# Build stage
FROM maven:3.9.5-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -B

# Runtime stage
FROM eclipse-temurin:17-jre

# Gerekli araçlar ve Google Chrome (ARM64 uyumlu, headless için gerekli bağımlılıklarla)
RUN set -e; \
    apt-get update && \
    apt-get install -y wget gnupg2 nodejs npm curl \
    fonts-liberation libasound2t64 libu2f-udev \
    libatk-bridge2.0-0 libatk1.0-0 libcups2 libdbus-1-3 \
    libgdk-pixbuf2.0-0 libnspr4 libnss3 libx11-xcb1 libxcomposite1 \
    libxdamage1 libxrandr2 libgbm1 libgtk-3-0 libxshmfence1 libxss1 libxtst6 \
    --no-install-recommends; \gbm1 libgtk-3-0 libxshmfence1 libxss1 libxtst6 \
    ARCH=$(dpkg --print-architecture); \
    if [ "$ARCH" = "amd64" ]; then \
      wget -O /tmp/google-chrome.deb https://dl.google.com/linux/direct/google-chrome-stable_current_amd64.deb && \
      apt-get install -y /tmp/google-chrome.deb && \
      rm -f /tmp/google-chrome.deb && \
      google-chrome --version; \stall -g lighthouse@11.1.0 && npm cache clean --force
      export CHROME_PATH=/usr/bin/google-chrome; \
    elif [ "$ARCH" = "arm64" ]; then \ dizinler
      apt-get install -y chromium-browser && \
      chromium-browser --version; \
      export CHROME_PATH=/usr/bin/chromium-browser; \t kullanıcı
    else \ -d /home/appuser appuser && \
      echo "Unsupported architecture: $ARCH" && exit 1; \ -R appuser:appuser /tmp/lighthouse /tmp/netty /home/appuser
    fi; \
    rm -rf /var/lib/apt/lists/*
COPY --from=builder /app/target/*.jar /app/app.jar
# Lighthouse CLI yükleser /app/app.jar
RUN npm install -g lighthouse@11.1.0 && npm cache clean --force
# Healthcheck script
# Gerekli dizinlerh /app/healthcheck.sh
RUN mkdir -p /tmp/lighthouse /tmp/nettywn appuser:appuser /app/healthcheck.sh

# Non-root kullanıcı
RUN groupadd -r appuser && useradd -r -g appuser -d /home/appuser appuser && \
    mkdir -p /home/appuser && chown -R appuser:appuser /tmp/lighthouse /tmp/netty /home/appuser
    CHROME_PATH=/usr/bin/chromium-browser \
# JAR dosyasını kopyala_PATH=/usr/bin/chromium-browser \
COPY --from=builder /app/target/*.jar /app/app.jar
RUN chown appuser:appuser /app/app.jarocal/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin"

# Healthcheck script
COPY healthcheck.sh /app/healthcheck.sh
RUN chmod +x /app/healthcheck.sh && chown appuser:appuser /app/healthcheck.sh

# Ortam değişkenlerial=30s --timeout=10s --start-period=60s --retries=3 CMD [ "bash", "/app/healthcheck.sh" ]
ENV JAVA_HOME=/opt/java/openjdk \
    NODE_PATH=/usr/local/lib/node_modules \
    CHROME_PATH=${CHROME_PATH:-/usr/bin/google-chrome} \    LIGHTHOUSE_CHROMIUM_PATH=${CHROME_PATH:-/usr/bin/google-chrome} \    PUPPETEER_SKIP_CHROMIUM_DOWNLOAD=true \    PATH="/opt/java/openjdk/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin"USER appuserWORKDIR /appEXPOSE 8085HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 CMD [ "bash", "/app/healthcheck.sh" ]CMD ["java", "-jar", "app.jar"]