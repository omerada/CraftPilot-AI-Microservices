# Craftpilot MongoDB Servisi

Bu bileşen, Craftpilot SaaS platformu için MongoDB veritabanı hizmetini sunar. Docker Compose kullanılarak yapılandırılmış olup, kolay dağıtım, yönetim ve ölçeklendirme imkanı sağlar.

## Özellikler

- Docker container içinde çalışan MongoDB (6.0)
- Mongo Express yönetim arayüzü
- Host sistemle entegre edilen volume'lar ile kolay veri yedekleme
- Kullanıcı kimlik doğrulama ve yetkilendirme
- Otomatik backup ve restore komutları
- CI/CD entegrasyonu
- Performans için optimize edilmiş konfigürasyon

## Kurulum

### Ön Gereksinimler

- Docker
- Docker Compose
- Make (opsiyonel, ancak önerilir)

### Adımlar

1. Repo'yu klonlayın
2. MongoDB servis dizinine gidin:
   ```
   cd mongodb-service
   ```
3. .env.example dosyasını kopyalayın:
   ```
   cp .env.example .env
   ```
4. .env dosyasını düzenleyerek gerekli şifreleri ve konfigürasyonu belirleyin
5. Servisi başlatın:

   ```
   make up
   ```

   veya

   ```
   docker-compose up -d
   ```

## Kullanım

### Temel Komutlar

Hizmetleri başlatmak:
