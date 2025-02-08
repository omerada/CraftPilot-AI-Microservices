# Build stage
FROM public.ecr.aws/amazoncorretto/amazoncorretto:21 AS build

WORKDIR /build

# Maven kurulumu için gerekli dizinleri oluştur
RUN mkdir -p /opt/maven

# Maven'i indir ve kur
RUN yum install -y wget tar gzip \
    && wget https://dlcdn.apache.org/maven/maven-3/3.9.6/binaries/apache-maven-3.9.6-bin.tar.gz \
    && tar xf apache-maven-3.9.6-bin.tar.gz -C /opt/maven --strip-components=1 \
    && rm -f apache-maven-3.9.6-bin.tar.gz

# Maven environment variables
ENV MAVEN_HOME /opt/maven
ENV PATH ${MAVEN_HOME}/bin:${PATH}

# Kaynak kodları kopyala
COPY . .

# Maven ile build
RUN mvn clean package -DskipTests

# Run stage
FROM public.ecr.aws/amazoncorretto/amazoncorretto:21

WORKDIR /app
COPY --from=build /build/target/*.jar app.jar

# JVM ayarları
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

# Sensitive data için volume kullan
VOLUME /etc/secrets
ENV GOOGLE_APPLICATION_CREDENTIALS=/etc/secrets/gcp-credentials.json

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dspring.profiles.active=prod -jar app.jar"]