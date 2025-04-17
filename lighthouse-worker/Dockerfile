FROM node:18-alpine

# Chrome yükleme
RUN apk add --no-cache \
    chromium \
    nss \
    freetype \
    harfbuzz \
    ttf-freefont \
    font-noto-emoji \
    && rm -rf /var/cache/apk/*

# Lighthouse Worker uygulama dizini oluştur
WORKDIR /app

# Önce package.json ve package-lock.json kopyala
COPY package*.json ./

# Bağımlılıkları yükle
RUN npm ci --only=production

# Uygulama kaynak kodlarını kopyala
COPY . .

# Geçici dosya dizini oluştur
RUN mkdir -p /tmp/lighthouse

# Çalışacak kullanıcı oluştur
RUN addgroup -S appuser && adduser -S appuser -G appuser \
    && chown -R appuser:appuser /app /tmp/lighthouse

# Chromium ve Lighthouse için çevre değişkenleri ayarla
ENV CHROME_PATH=/usr/bin/chromium-browser \
    LIGHTHOUSE_CHROMIUM_PATH=/usr/bin/chromium-browser \
    PUPPETEER_SKIP_CHROMIUM_DOWNLOAD=true \
    NODE_ENV=production

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=30s --retries=3 CMD wget -q -O /dev/null http://localhost:8086/health || exit 1

# Kullanıcı değiştir
USER appuser

# Uygulama portu
EXPOSE 8086

# Uygulamayı çalıştır
CMD ["node", "src/index.js"]
