# 1. Aşama: Commons kütüphanesini derle
FROM maven:3.8.6-eclipse-temurin-21 AS commons-builder
WORKDIR /app/commons
COPY craft-pilot-commons/pom.xml .
COPY craft-pilot-commons/src ./src
RUN mvn clean install -DskipTests

# 2. Aşama: LLM Service'i derle
FROM maven:3.8.6-eclipse-temurin-21 AS llm-builder
WORKDIR /app/llm-service
COPY llm-service/pom.xml .
# Maven bağımlılıklarını önce indiriyoruz (cache'leme için)
RUN mvn dependency:go-offline

# Commons kütüphanesini maven local repo'ya kopyala
COPY --from=commons-builder /root/.m2/repository/com/craftpilot /root/.m2/repository/com/craftpilot

# Kaynak kodları ve derle
COPY llm-service/src ./src
RUN mvn clean package -DskipTests

# 3. Aşama: Runtime image
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=llm-builder /app/llm-service/target/*.jar llm-service.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "llm-service.jar"]