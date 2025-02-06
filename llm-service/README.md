# LLM Service

Bu servis, Craft Pilot platformunda yapay zeka destekli soru üretimi ve yönetiminden sorumludur.

## Özellikler

- OpenAI GPT-4 entegrasyonu ile akıllı soru üretimi
- Firestore veritabanı ile soru ve yanıt yönetimi
- Reactive programlama ile yüksek performanslı API'lar
- Rate limiting ve circuit breaker koruması
- Swagger UI ile API dokümantasyonu
- Eureka service discovery entegrasyonu
- Kubernetes deployment desteği

## Gereksinimler

- Java 21
- Maven 3.8+
- Docker (opsiyonel)
- Firebase hesabı ve servis hesap anahtarı
- OpenAI API anahtarı

## Kurulum

1. Gerekli ortam değişkenlerini ayarlayın:

```bash
export OPENAI_API_KEY=your-openai-api-key
export FIREBASE_PRIVATE_KEY_ID=your-firebase-private-key-id
export FIREBASE_PRIVATE_KEY=your-firebase-private-key
export FIREBASE_CLIENT_EMAIL=your-firebase-client-email
export FIREBASE_CLIENT_ID=your-firebase-client-id
export FIREBASE_CLIENT_CERT_URL=your-firebase-client-cert-url
```

2. Projeyi derleyin:

```bash
./mvnw clean install
```

3. Servisi başlatın:

```bash
./mvnw spring-boot:run
```

## API Dokümantasyonu

Swagger UI'a http://localhost:8082/swagger-ui.html adresinden erişebilirsiniz.

## Metrikler

Prometheus metriklerine http://localhost:8082/actuator/prometheus adresinden erişebilirsiniz.

## Sağlık Kontrolü

Servis sağlık durumunu http://localhost:8082/actuator/health adresinden kontrol edebilirsiniz.

## Docker ile Çalıştırma

```bash
docker build -t llm-service .
docker run -p 8082:8082 llm-service
```

## Kubernetes ile Deploy

```bash
kubectl apply -f k8s/
```

## Geliştirme

1. Yeni bir özellik eklemek için branch oluşturun:

```bash
git checkout -b feature/yeni-ozellik
```

2. Değişikliklerinizi commit edin:

```bash
git commit -am 'Yeni özellik: özellik açıklaması'
```

3. Pull request oluşturun

## Test

```bash
./mvnw test
```

## Lisans

Bu proje MIT lisansı altında lisanslanmıştır. Detaylar için [LICENSE](LICENSE) dosyasına bakın.
