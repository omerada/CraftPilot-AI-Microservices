# Chrome ve Lighthouse kurulumu için temel imaj
FROM node:16-slim AS node-base

ARG TARGETARCH
ARG CHROME_ARCH=${TARGETARCH:-arm64}

# Gerekli sistem paketlerini yükle ve Lighthouse kurulumu
RUN apt-get update && apt-get install -y \
    chromium \
    libxss1 \
    ca-certificates \
    wget \
    gnupg \
    apt-transport-https \
    procps \
    && npm install -g lighthouse@9.6.8 puppeteer@13.5.1 \
    && apt-get clean && rm -rf /var/lib/apt/lists/*

# Chromium alias’ı oluştur (Lighthouse’un kullanabilmesi için)
RUN ln -s /usr/bin/chromium /usr/bin/google-chrome

# Java uygulaması için builder aşaması
FROM eclipse-temurin:17-jdk AS builder
WORKDIR /app

COPY mvnw ./
COPY .mvn .mvn
COPY pom.xml ./
COPY src ./src

RUN ./mvnw clean package -DskipTests

# Final imaj (sadece çalışma için gerekli her şey)
FROM eclipse-temurin:21-jre

# Node ve Lighthouse CLI
COPY --from=node-base /usr/local/bin/node /usr/local/bin/
COPY --from=node-base /usr/local/bin/npm /usr/local/bin/
COPY --from=node-base /usr/local/bin/lighthouse /usr/local/bin/
COPY --from=node-base /usr/local/lib/node_modules /usr/local/lib/node_modules
COPY --from=node-base /usr/bin/chromium /usr/bin/chromium
COPY --from=node-base /usr/bin/google-chrome /usr/bin/google-chrome

# Java uygulaması
COPY --from=builder /app/target/*.jar /app/app.jar

# Geçici klasör ve kullanıcı ayarları
RUN mkdir -p /tmp/lighthouse && \
    useradd -m -d /home/appuser -s /bin/bash appuser && \
    chown -R appuser:appuser /app /tmp/lighthouse && \
    chmod -R 777 /tmp/lighthouse

USER appuser

WORKDIR /app
EXPOSE 8085

ENTRYPOINT ["java", "-jar", "app.jar"]
