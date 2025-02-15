# Build stage
FROM public.ecr.aws/amazoncorretto/amazoncorretto:21 AS build

WORKDIR /build

# Gerekli araçları yükle
RUN yum install -y tar gzip

# Maven wrapper ve bağımlılıkları kopyala
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw
RUN ./mvnw dependency:go-offline

# Kaynak kodları kopyala ve derle
COPY src ./src/
RUN ./mvnw clean package -DskipTests

# Run stage
FROM public.ecr.aws/amazoncorretto/amazoncorretto:21

WORKDIR /app

# Uygulama dizinini oluştur
RUN mkdir -p /app/config

# JAR dosyasını kopyala
COPY --from=build /build/target/*.jar app.jar

# curl yükle (health check için)
RUN yum install -y curl

# JVM ayarları
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

# Sağlık kontrolü için bekleme
HEALTHCHECK --interval=10s --timeout=5s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8063/actuator/health || exit 1

EXPOSE 8063
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]