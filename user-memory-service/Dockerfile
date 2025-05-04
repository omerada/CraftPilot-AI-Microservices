# Build stage
FROM eclipse-temurin:21-jdk as build
WORKDIR /workspace/app

# Önce bağımlılıkları kopyalayıp derleyelim (önbellek kullanmak için)
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY src src

# Maven ile paketleme işlemi
RUN ./mvnw package -DskipTests
RUN mkdir -p target/dependency && (cd target/dependency; jar -xf ../*.jar)

# Production stage
FROM eclipse-temurin:21-jre
VOLUME /tmp
ARG DEPENDENCY=/workspace/app/target/dependency

# Google Cloud için kimlik doğrulama dosyaları için bir dizin oluştur
RUN mkdir -p /secrets

# Non-root kullanıcı ile çalıştır
RUN addgroup --system --gid 1001 craftpilot && \
    adduser --system --uid 1001 --ingroup craftpilot craftpilot
USER craftpilot:craftpilot

# Jar dosyasını katmanlı olarak kopyala
COPY --from=build ${DEPENDENCY}/BOOT-INF/lib /app/lib
COPY --from=build ${DEPENDENCY}/META-INF /app/META-INF
COPY --from=build ${DEPENDENCY}/BOOT-INF/classes /app

# Healthcheck yapılandırması
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8067/actuator/health || exit 1

# Prometheus metrik toplayıcılar için gerekli port
EXPOSE 8067

# Uygulama çalıştırma komutu
ENTRYPOINT ["java", "-cp", "app:app/lib/*", "com.craftpilot.usermemoryservice.UserMemoryServiceApplication"]
