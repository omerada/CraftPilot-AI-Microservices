# Maven build stage
FROM maven:3.9-eclipse-temurin-21-jammy AS builder
WORKDIR /app

# Maven wrapper ve bağımlılıkları kopyala
COPY mvnw ./
COPY .mvn .mvn
COPY pom.xml ./

# Bağımlılıkları indir
RUN chmod +x ./mvnw && ./mvnw dependency:go-offline -B

# Kodu kopyala ve derle
COPY src ./src
RUN ./mvnw clean package -DskipTests -B

# Final image
FROM eclipse-temurin:21-jre-alpine

# Temel araçları kur
RUN apk add --no-cache \
    nodejs \
    npm \
    chromium \
    curl \
    bash \
    # Gerekli fontları ekle
    ttf-freefont \
    # Gerekli araçları ekle
    dumb-init

# Lighthouse CLI kurulumu
RUN npm install -g lighthouse@11.1.0 --unsafe-perm && \
    npm cache clean --force && \
    # Basit sembolik link ekle
    ln -sf $(which chromium-browser) /usr/bin/chromium || true && \
    ln -sf $(which chromium-browser) /usr/bin/google-chrome || true

# Çevre değişkenleri
ENV NODE_PATH=/usr/local/lib/node_modules \
    CHROME_PATH=/usr/bin/chromium-browser \
    PATH="/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:/opt/java/openjdk/bin"

# Geçici klasörleri oluştur
RUN mkdir -p /tmp/lighthouse /home/appuser && \
    addgroup -S appuser && adduser -S -G appuser appuser && \
    chown -R appuser:appuser /tmp/lighthouse /home/appuser

# Uygulamayı kopyala
COPY --from=builder /app/target/*.jar /app/app.jar
RUN chown appuser:appuser /app/app.jar

# Test et
RUN echo "Checking Java installation" && \
    java -version && \
    echo "Checking Node installation" && \
    node --version && \
    echo "Checking Lighthouse CLI" && \
    which lighthouse || echo "Lighthouse CLI not in PATH" && \
    lighthouse --version || echo "Lighthouse version check failed, continuing anyway"

# Kullanıcı değiştir
USER appuser
WORKDIR /app

# Portu aç ve uygulamayı başlat
EXPOSE 8085
ENTRYPOINT ["/usr/bin/dumb-init", "--"]
CMD ["java", "-jar", "app.jar"]
