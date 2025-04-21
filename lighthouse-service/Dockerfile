# 1. Aşama: Commons kütüphanesini derle
FROM maven:3.8.6-eclipse-temurin-17 AS commons-builder
WORKDIR /app/commons
COPY craft-pilot-commons/pom.xml .
COPY craft-pilot-commons/src ./src
RUN mvn clean install -DskipTests

# 2. Aşama: Lighthouse Service'i derle
FROM maven:3.8.6-eclipse-temurin-17 AS lighthouse-builder
WORKDIR /app/lighthouse-service
COPY lighthouse-service/pom.xml .
# Maven bağımlılıklarını önce indiriyoruz (cache'leme için)
RUN mvn dependency:go-offline

# Commons kütüphanesini maven local repo'ya kopyala
COPY --from=commons-builder /root/.m2/repository/com/craftpilot /root/.m2/repository/com/craftpilot

# Kaynak kodları kopyala ve derle
COPY lighthouse-service/src ./src
RUN mvn clean package -DskipTests

# 3. Aşama: Runtime image
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=lighthouse-builder /app/lighthouse-service/target/*.jar lighthouse-service.jar
EXPOSE 8085
ENTRYPOINT ["java", "-jar", "lighthouse-service.jar"]