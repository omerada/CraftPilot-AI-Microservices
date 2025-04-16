# Node.js ve Lighthouse kurulumu için temel imaj
FROM node:16-slim AS node-base

# Gerekli paketleri kur ve Lighthouse'u global olarak yükle
RUN apt-get update \
    && apt-get install -y wget gnupg ca-certificates procps libxss1 \
    && wget -q -O - https://dl-ssl.google.com/linux/linux_signing_key.pub | apt-key add - \
    && sh -c 'echo "deb [arch=amd64] http://dl.google.com/linux/chrome/deb/ stable main" >> /etc/apt/sources.list.d/google.list' \
    && apt-get update \
    && apt-get install -y google-chrome-stable \
    && npm install -g lighthouse@9.6.8 \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

# Java uygulaması için temel imaj
FROM eclipse-temurin:17-jdk as builder

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
COPY --from=node-base /opt/google /opt/google
COPY --from=node-base /usr/bin/google-chrome /usr/bin/

WORKDIR /app

# Java uygulamasını kopyala
COPY --from=builder /app/target/*.jar app.jar

# Çalışma dizinini ve kullanıcıyı oluştur
RUN mkdir -p /tmp/lighthouse && \
    useradd -m -d /home/appuser -s /bin/bash appuser && \
    chown -R appuser:appuser /app /tmp/lighthouse

# Gerekli izinleri ayarla
RUN chmod 755 /usr/bin/google-chrome && \
    chmod -R 777 /tmp/lighthouse

# Kullanıcıyı değiştir
USER appuser

# Uygulama portunu belirle
EXPOSE 8085

# Uygulamayı başlat
ENTRYPOINT ["java", "-jar", "app.jar"]
