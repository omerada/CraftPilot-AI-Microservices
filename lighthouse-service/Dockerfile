# Build stage
FROM maven:3.9.5-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -B

# Runtime stage
FROM eclipse-temurin:17-jre

# Gerekli araçlar: nodejs, npm, curl
RUN apt-get update && \
    apt-get install -y nodejs npm curl && \
    rm -rf /var/lib/apt/lists/*

# Google Chrome stable yükle (daha kararlı ve Lighthouse ile uyumlu)
RUN curl -fsSL https://dl.google.com/linux/linux_signing_key.pub | gpg --dearmor -o /usr/share/keyrings/google-linux.gpg && \
    echo "deb [arch=amd64 signed-by=/usr/share/keyrings/google-linux.gpg] http://dl.google.com/linux/chrome/deb/ stable main" > /etc/apt/sources.list.d/google-chrome.list && \
    apt-get update && \
    apt-get install -y google-chrome-stable && \
    rm -rf /var/lib/apt/lists/*

# Lighthouse CLI yükle
RUN npm install -g lighthouse@11.1.0 && npm cache clean --force

# Gerekli dizinler
RUN mkdir -p /tmp/lighthouse /tmp/netty

# Non-root kullanıcı
RUN groupadd -r appuser && useradd -r -g appuser -d /home/appuser appuser && \
    mkdir -p /home/appuser && chown -R appuser:appuser /tmp/lighthouse /tmp/netty /home/appuser

# JAR dosyasını kopyala
COPY --from=builder /app/target/*.jar /app/app.jar
RUN chown appuser:appuser /app/app.jar

# Healthcheck script
COPY healthcheck.sh /app/healthcheck.sh
RUN chmod +x /app/healthcheck.sh && chown appuser:appuser /app/healthcheck.sh

# Ortam değişkenleri
ENV NODE_PATH=/usr/local/lib/node_modules \
    CHROME_PATH=/usr/bin/google-chrome \
    LIGHTHOUSE_CHROMIUM_PATH=/usr/bin/google-chrome \
    PUPPETEER_SKIP_CHROMIUM_DOWNLOAD=true \
    PATH="/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin"

USER appuser
WORKDIR /app
EXPOSE 8085

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 CMD [ "bash", "/app/healthcheck.sh" ]

CMD ["java", "-jar", "app.jar"]
