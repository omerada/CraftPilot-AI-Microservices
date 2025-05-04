# 1. Builder aşaması: Java uygulamasını derle
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

# Bağımlılıkları önce kopyala ve derle (önbellek optimizasyonu için)
COPY pom.xml .
COPY mvnw .
COPY .mvn .mvn
RUN chmod +x ./mvnw
RUN ./mvnw dependency:go-offline -B

# Kaynak kodları kopyala ve derle
COPY src src
RUN ./mvnw package -DskipTests
    
# 2. Çalışma aşaması: Sadece JDK ile çalıştırma (JRE-alpine ARM64 için mevcut değil)
FROM eclipse-temurin:21-jdk-alpine AS runtime
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar

# Sağlık kontrolü için curl ekle
RUN apk add --no-cache curl

# Uygulama'yı çalıştır
ENTRYPOINT ["java", "-jar", "app.jar"]
