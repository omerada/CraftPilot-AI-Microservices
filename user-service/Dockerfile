# Redis Client Library Build Stage
FROM maven:3.9.6-eclipse-temurin-21-alpine AS redis-lib-build

# Maven ayarlarını optimize et
ENV MAVEN_OPTS="-XX:+TieredCompilation -XX:TieredStopAtLevel=1 -Xmx512m"

# Clone/copy the redis-client-lib source
WORKDIR /build-lib
COPY redis-client-lib ./redis-client-lib

# Build and install the redis-client-lib to local Maven repository
RUN cd redis-client-lib && mvn clean install -DskipTests -B -ntp

# Build stage
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder

# Maven ayarlarını optimize et
ENV MAVEN_OPTS="-XX:+TieredCompilation -XX:TieredStopAtLevel=1 -Xmx2g"

WORKDIR /build

# Temizlik adımı ekleyin - build artefaktlarını ve cache'i temizler
RUN rm -rf /root/.m2/repository/com/craftpilot/userservice/

# Copy Redis client lib first
COPY redis-client-lib/ /build/redis-client-lib/

# Maven dependency cache oluşturma
COPY user-service/pom.xml /build/user-service/
RUN cd /build/redis-client-lib && mvn clean install -DskipTests -B -ntp && \
    cd /build/user-service && mvn dependency:go-offline -B -ntp

# Copy user-service
COPY user-service/ /build/user-service/

# Build user-service - MapStruct sorununu çözen iyileştirilmiş derleme adımı
WORKDIR /build/user-service
RUN rm -rf target && \
    rm -rf /root/.m2/repository/com/craftpilot/userservice/ && \
    find . -name "*.class" -delete && \
    find . -name "MapperImpl.java" -delete && \
    find . -name "*Mapper*.java" -not -name "UserMapper.java" -not -name "UserPreferenceMapper.java" -delete && \
    mvn clean package -Dmaven.compiler.forceJavacCompilerUse=true -Dmapstruct.defaultComponentModel=spring -Dmapstruct.verbose=true -DskipTests -B -ntp

# Run stage
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy jar file
COPY --from=builder /build/user-service/target/*.jar app.jar

# Çalışma zamanı ayarları
ENV SPRING_PROFILES_ACTIVE=prod
ENV SPRING_DATA_MONGODB_AUTO_INDEX_CREATION=false
ENV SPRING_APPLICATION_MONGODB_URI=""
ENV SPRING_APPLICATION_MONGODB_DATABASE=user-service

# Health check ekliyorum
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget -q --no-check-certificate -O /dev/null http://localhost:8080/actuator/health || exit 1

# App config dizini oluştur
RUN mkdir -p /app/config

# Expose port
EXPOSE 8080

# Basitleştirilmiş entrypoint - Firebase kaldırıldığından entrypoint.sh yerine doğrudan JAR'ı çalıştırıyoruz
CMD ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]