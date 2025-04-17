# Build stage
FROM maven:3.9.5-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -B

# Runtime stage
FROM eclipse-temurin:17-jre

RUN set -e; \
    apt-get update && \
    apt-get install -y wget gnupg2 nodejs npm curl \
    fonts-liberation libasound2t64 libu2f-udev \
    chromium-browser \
    libatk-bridge2.0-0 libatk1.0-0 libcups2 libdbus-1-3 \
    libgdk-pixbuf2.0-0 libnspr4 libnss3 libx11-xcb1 libxcomposite1 \
    libxdamage1 libxrandr2 libgbm1 libgtk-3-0 libxshmfence1 libxss1 libxtst6 \
    --no-install-recommends; \
    rm -rf /var/lib/apt/lists/*; \
    chromium-browser --version

RUN npm install -g lighthouse@11.1.0 && npm cache clean --force

RUN mkdir -p /tmp/lighthouse /tmp/netty

RUN groupadd -r appuser && useradd -r -g appuser -d /home/appuser appuser && \
    mkdir -p /home/appuser && chown -R appuser:appuser /tmp/lighthouse /tmp/netty /home/appuser

COPY --from=builder /app/target/*.jar /app/app.jar
RUN chown appuser:appuser /app/app.jar

COPY healthcheck.sh /app/healthcheck.sh
RUN chmod +x /app/healthcheck.sh && chown appuser:appuser /app/healthcheck.sh

ENV JAVA_HOME=/opt/java/openjdk \
    NODE_PATH=/usr/local/lib/node_modules \
    CHROME_PATH=/usr/bin/chromium-browser \
    LIGHTHOUSE_CHROMIUM_PATH=/usr/bin/chromium-browser \
    PUPPETEER_SKIP_CHROMIUM_DOWNLOAD=true \
    PATH="/opt/java/openjdk/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin"

USER appuser
WORKDIR /app
EXPOSE 8085

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 CMD [ "bash", "/app/healthcheck.sh" ]

CMD ["java", "-jar", "app.jar"]