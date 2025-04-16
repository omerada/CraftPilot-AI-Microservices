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
FROM eclipse-temurin:21-jre-alpine

# Alpine paketleri ile gerekli bağımlılıkları kur
RUN apk add --no-cache --update \
    chromium \
    nodejs \
    npm \
    fontconfig \
    freetype \
    ttf-dejavu \
    ttf-droid \
    ttf-freefont \
    ttf-liberation \
    curl \
    bash \
    dumb-init

# Lighthouse ve bağımlılıkları global olarak yükle (daha az paket)
RUN npm install -g lighthouse@11.1.0 chrome-launcher@0.15.2 && \
    npm cache clean --force

# Gerekli sembolik linkler ve dizinler 
RUN ln -s /usr/bin/chromium-browser /usr/bin/chromium && \
    ln -s /usr/bin/chromium-browser /usr/bin/google-chrome && \
    mkdir -p /tmp/lighthouse /home/appuser

# Lighthouse için ayarlar
ENV NODE_PATH=/usr/local/lib/node_modules \
    CHROME_PATH=/usr/bin/chromium \
    LIGHTHOUSE_CHROMIUM_PATH=/usr/bin/chromium \
    PUPPETEER_SKIP_CHROMIUM_DOWNLOAD=true

# Kullanıcı oluştur (güvenlik için)
RUN addgroup -S appgroup && adduser -S -G appgroup appuser && \
    chown -R appuser:appgroup /tmp/lighthouse /home/appuser

# Java uygulamasını kopyala
COPY --from=builder /app/target/*.jar /app/app.jar
RUN chown appuser:appgroup /app/app.jar

# Kullanıcı değiştir
USER appuser
WORKDIR /app
EXPOSE 8085

CMD ["java", "-jar", "app.jar"]
