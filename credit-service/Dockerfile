# Build stage
FROM public.ecr.aws/amazoncorretto/amazoncorretto:21 AS build

WORKDIR /build

# Tüm projeyi kopyala
COPY . .

# Maven kurulumu
RUN yum install -y maven

# Maven build için platform ayarı
ARG TARGETARCH=arm64
RUN echo "Building for ARM64 architecture"

# Projeyi derle
RUN mvn clean package -DskipTests

# Run stage
FROM public.ecr.aws/amazoncorretto/amazoncorretto:21-alpine

WORKDIR /app
COPY --from=build /build/target/*.jar app.jar

# JVM ayarları
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"
RUN echo "Running on ARM64 architecture"

EXPOSE 8086
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dspring.profiles.active=prod -jar app.jar"]