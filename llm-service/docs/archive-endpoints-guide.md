# Sohbet Arşivleme İşlemleri API Dokümanı

Bu doküman, sohbet geçmişlerinin arşivlenmesi, arşivden çıkarılması ve arşivlenmiş sohbetlerin listelenmesi ile ilgili API endpointlerini açıklar.

## Endpointler

### 1. Sohbet Arşivleme

Bir sohbet geçmişini arşive ekler.

- **Endpoint**: `POST /chat/histories/{id}/do-archive`
- **Path Parametreleri**:
  - `id`: Arşivlenmek istenen sohbet geçmişinin ID'si
- **Headers**:
  - `X-User-Id`: Kullanıcı kimliği (zorunlu)
- **Başarılı Yanıt**: `HTTP 200 OK`
  - Arşivlenen sohbet geçmişi detayları

```json
{
  "id": "sohbet123",
  "userId": "kullanici456",
  "title": "Arşivlenen Sohbet",
  "createdAt": { ... },
  "updatedAt": { ... },
  "conversations": [ ... ],
  "enable": false
}
```

### 2. Sohbet Arşivden Çıkarma

Arşivlenmiş bir sohbeti arşivden çıkarır.

- **Endpoint**: `POST /chat/histories/{id}/do-unarchive`
- **Path Parametreleri**:
  - `id`: Arşivden çıkarılmak istenen sohbet geçmişinin ID'si
- **Headers**:
  - `X-User-Id`: Kullanıcı kimliği (zorunlu)
- **Başarılı Yanıt**: `HTTP 200 OK`
  - Arşivden çıkarılan sohbet geçmişi detayları

```json
{
  "id": "sohbet123",
  "userId": "kullanici456",
  "title": "Arşivden Çıkarılan Sohbet",
  "createdAt": { ... },
  "updatedAt": { ... },
  "conversations": [ ... ],
  "enable": true
}
```

### 3. Arşivlenmiş Sohbetleri Listeleme

Kullanıcının arşivlediği tüm sohbetleri listeler.

- **Endpoint**: `GET /chat/archived-histories`
- **Query Parametreleri**:
  - `userId`: Kullanıcı kimliği (zorunlu)
  - `page`: Sayfa numarası (varsayılan: 1)
  - `pageSize`: Sayfa başına sonuç sayısı (varsayılan: 10)
  - `searchQuery`: Başlıklarda arama yapmak için (opsiyonel)
  - `sortBy`: Sıralama kriteri, "createdAt" veya "updatedAt" (varsayılan: "updatedAt")
  - `sortOrder`: Sıralama yönü, "asc" veya "desc" (varsayılan: "desc")
- **Başarılı Yanıt**: `HTTP 200 OK`

```json
{
  "categories": {
    "archived": {
      "items": [
        {
          "id": "sohbet123",
          "title": "Arşivlenen Sohbet 1",
          "createdAt": { ... },
          "updatedAt": { ... },
          "lastConversation": "Son mesaj içeriği..."
        },
        // ... diğer arşivlenen sohbetler
      ],
      "count": 5
    }
  },
  "pagination": {
    "currentPage": 1,
    "totalPages": 1,
    "pageSize": 10,
    "totalItems": 5,
    "hasMore": false
  }
}
```

### 4. Arşiv Durumuna Göre Sohbetleri Filtreleme

Mevcut sohbetleri arşiv durumuna göre filtreleme.

- **Endpoint**: `GET /chat/histories`
- **Query Parametreleri**:
  - `userId`: Kullanıcı kimliği (zorunlu)
  - `page`: Sayfa numarası (varsayılan: 1)
  - `pageSize`: Sayfa başına sonuç sayısı (varsayılan: 10)
  - `categories`: Kategoriler (varsayılan: tüm kategoriler)
  - `searchQuery`: Başlıklarda arama yapmak için (opsiyonel)
  - `sortBy`: Sıralama kriteri (varsayılan: "updatedAt")
  - `sortOrder`: Sıralama yönü (varsayılan: "desc")
  - `showArchived`: Arşivlenmiş sohbetleri göster/gizle (opsiyonel)
    - `true`: Sadece arşivlenmiş sohbetleri göster
    - `false`: Sadece arşivlenmemiş (aktif) sohbetleri göster
    - gönderilmezse: Tüm sohbetleri göster
- **Başarılı Yanıt**: `HTTP 200 OK`
  - Standart sohbet listesi yanıtı

### 5. Düz Liste Halinde Sohbet Geçmişini Arşiv Durumuna Göre Filtreleme

ChatGPT formatında düz liste halinde sohbet geçmişlerini arşiv durumuna göre filtreleme.

- **Endpoint**: `GET /chat/flat-histories`
- **Query Parametreleri**:
  - `userId`: Kullanıcı kimliği (zorunlu)
  - `offset`: Atlanacak kayıt sayısı (varsayılan: 0)
  - `limit`: Getirilecek kayıt sayısı (varsayılan: 20)
  - `order`: Sıralama kriteri, "created" veya "updated" (varsayılan: "updated")
  - `showArchived`: Arşivlenmiş sohbetleri göster/gizle (opsiyonel)
    - `true`: Sadece arşivlenmiş sohbetleri göster
    - `false`: Sadece arşivlenmemiş (aktif) sohbetleri göster
    - gönderilmezse: Tüm sohbetleri göster
- **Başarılı Yanıt**: `HTTP 200 OK`

```json
{
  "items": [
    {
      "id": "sohbet123",
      "title": "Sohbet Başlığı",
      "create_time": "2023-11-01T09:30:00Z",
      "update_time": "2023-11-01T10:15:00Z",
      "is_archived": true,
      "snippet": "Son mesajın bir kısmı..."
    }
    // ... diğer sohbet geçmişleri
  ],
  "total": 35,
  "limit": 20,
  "offset": 0
}
```

## Hata Durumları

| Durum Kodu | Açıklama                  |
| ---------- | ------------------------- |
| 404        | Sohbet geçmişi bulunamadı |
| 500        | Sunucu hatası             |

## Teknik Notlar

- Arşivleme işlemi, ChatHistory modelindeki `enable` alanını `false` olarak işaretleyerek gerçekleştirilir.
- Arşivden çıkarma işlemi, ChatHistory modelindeki `enable` alanını `true` olarak işaretleyerek gerçekleştirilir.
- Arşivlenmiş sohbetlerin `is_archived` değeri `true` olarak görünür (bu, `enable=false` durumunu temsil eder).
