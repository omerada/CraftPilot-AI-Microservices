# Build stage
FROM public.ecr.aws/amazoncorretto/amazoncorretto:21 AS build

WORKDIR /build

# Tüm projeyi kopyala
COPY . .

# Maven kurulumu ve versiyonu belirleme
RUN yum install -y wget tar gzip \
    && wget https://dlcdn.apache.org/maven/maven-3/3.9.6/binaries/apache-maven-3.9.6-bin.tar.gz \
    && tar -xzf apache-maven-3.9.6-bin.tar.gz -C /opt/ \
    && ln -s /opt/apache-maven-3.9.6 /opt/maven \
    && rm -f apache-maven-3.9.6-bin.tar.gz

ENV M2_HOME=/opt/maven
ENV MAVEN_HOME=/opt/maven
ENV PATH=${M2_HOME}/bin:${PATH}

# Maven build için platform ayarı
ARG TARGETARCH=arm64
RUN echo "Building for ARM64 architecture"

# Projeyi derle
RUN mvn clean package -DskipTests

# Run stage
FROM public.ecr.aws/amazoncorretto/amazoncorretto:21

WORKDIR /app
COPY --from=build /build/target/*.jar app.jar

# JVM ayarları
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"
RUN echo "Running on ARM64 architecture"

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dspring.profiles.active=prod -jar app.jar"]