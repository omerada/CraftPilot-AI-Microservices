# User Memory Service API Entegrasyon Dökümanı

## İçindekiler

1. [Genel Bilgiler](#genel-bilgiler)
2. [Endpointler](#endpointler)
   - [Kullanıcı Bellekleri](#kullanıcı-bellekleri)
     - [Bellek Girdisi Ekleme](#bellek-girdisi-ekleme)
     - [Kullanıcı Belleğini Getirme](#kullanıcı-belleğini-getirme)
     - [Bellek Girdisi Silme](#bellek-girdisi-silme)
     - [Tüm Bellek Girdilerini Silme](#tüm-bellek-girdilerini-silme)
   - [Kullanıcı Talimatları](#kullanıcı-talimatları)
     - [Talimatlari Getirme](#talimatları-getirme)
     - [Talimat Ekleme](#talimat-ekleme)
     - [Talimat Güncelleme](#talimat-güncelleme)
     - [Belirli Talimatı Silme](#belirli-talimatı-silme)
     - [Tüm Talimatları Silme](#tüm-talimatları-silme)
   - [Kullanıcı Bağlamı](#kullanıcı-bağlamı)
     - [Bağlam Bilgisini Getirme](#bağlam-bilgisini-getirme)
   - [Dil Stili Tercihleri](#dil-stili-tercihleri)
     - [Dil Stili Tercihlerini Getirme](#dil-stili-tercihlerini-getirme)
     - [Dil Stili Tercihlerini Güncelleme](#dil-stili-tercihlerini-güncelleme)
     - [Dil Stili Tercihlerini Silme](#dil-stili-tercihlerini-silme)
3. [Veri Modelleri](#veri-modelleri)
4. [Hata Kodları](#hata-kodları)
5. [Örnek İstek ve Yanıtlar](#örnek-istek-ve-yanıtlar)

## Genel Bilgiler

User Memory Service, kullanıcıların anlamlı bilgilerini saklamak, yönetmek ve erişim sağlamak için tasarlanmış bir servistir. Bu servis, kullanıcıların uygulamada yaptıkları etkileşimlere dayalı bilgileri depolar ve bu bilgileri diğer servisler için erişilebilir kılar.

**Temel URL**: `http://user-memory-service:8067` (Dahili ortamda) veya API Gateway üzerinden `/memories/**`

## Endpointler

### Kullanıcı Bellekleri

#### Bellek Girdisi Ekleme

**Endpoint**: `/memories/entries`  
**Metod**: POST  
**Açıklama**: Kullanıcı için yeni bir bellek girdisi ekler

**Header Parametreleri**:
| İsim | Gereklilik | Açıklama |
|------|------------|----------|
| X-User-Id | Zorunlu | Bellek girdisinin ekleneceği kullanıcının ID'si |

**İstek Gövdesi**:

```json
{
  "content": "Kullanıcının favori rengi mavi.",
  "source": "Kullanıcı ile sohbet",
  "context": "Renk tercihleri hakkında konuşma",
  "metadata": {
    "confidence": 0.85,
    "category": "tercihler"
  },
  "timestamp": "2023-10-15T14:30:45.123",
  "importance": 0.7
}
```

**Cevap Örneği - Başarılı (200 OK)**:

```json
{
  "message": "Memory entry added successfully",
  "success": true
}
```

**Olası Hatalar**:

- 403 Forbidden: Firebase yetkilendirme hatası
- 500 Internal Server Error: Bellek işleme hatası

#### Kullanıcı Belleğini Getirme

**Endpoint**: `/memories/{userId}`  
**Metod**: GET  
**Açıklama**: Belirli bir kullanıcının tüm bellek girişlerini getirir

**URL Parametreleri**:
| İsim | Gereklilik | Açıklama |
|------|------------|----------|
| userId | Zorunlu | Belleği getirilecek kullanıcının ID'si |

**Cevap Örneği - Başarılı (200 OK)**:

```json
{
  "id": "user123",
  "userId": "user123",
  "created": "2023-09-10T08:15:30",
  "lastUpdated": "2023-10-15T14:30:45.123",
  "entries": [
    {
      "content": "Kullanıcının favori rengi mavi.",
      "source": "Kullanıcı ile sohbet",
      "context": "Renk tercihleri hakkında konuşma",
      "metadata": {
        "confidence": 0.85,
        "category": "tercihler"
      },
      "timestamp": "2023-10-15T14:30:45.123",
      "importance": 0.7
    },
    {
      "content": "Kullanıcı kedileri seviyor.",
      "source": "AI analizi",
      "context": "Hayvanlar hakkında konuşma",
      "metadata": {
        "confidence": 0.9,
        "category": "ilgi_alanlari"
      },
      "timestamp": "2023-10-10T11:22:33.456",
      "importance": 0.5
    }
  ]
}
```

**Olası Hatalar**:

- 403 Forbidden: Firebase yetkilendirme hatası
- 500 Internal Server Error: Bellek alma hatası

#### Bellek Girdisi Silme

**Endpoint**: `/memories/{userId}/{entryIndex}`  
**Metod**: DELETE  
**Açıklama**: Kullanıcının belirli bir bellek girdisini siler

**URL Parametreleri**:
| İsim | Gereklilik | Açıklama |
|------|------------|----------|
| userId | Zorunlu | Bellek girdisi silinecek kullanıcının ID'si |
| entryIndex | Zorunlu | Silinecek bellek girdisinin dizindeki konumu (0 tabanlı) |

**Cevap Örneği - Başarılı (200 OK)**:

```json
{
  "message": "Memory entry deleted successfully",
  "success": true
}
```

**Olası Hatalar**:

- 400 Bad Request: Geçersiz indeks değeri
- 403 Forbidden: Firebase yetkilendirme hatası
- 500 Internal Server Error: Bellek silme hatası

#### Tüm Bellek Girdilerini Silme

**Endpoint**: `/memories/{userId}`  
**Metod**: DELETE  
**Açıklama**: Kullanıcının tüm bellek girdilerini siler

**URL Parametreleri**:
| İsim | Gereklilik | Açıklama |
|------|------------|----------|
| userId | Zorunlu | Tüm bellek girdileri silinecek kullanıcının ID'si |

**Cevap Örneği - Başarılı (200 OK)**:

```json
{
  "message": "All memory entries deleted successfully",
  "success": true
}
```

**Olası Hatalar**:

- 403 Forbidden: Firebase yetkilendirme hatası
- 500 Internal Server Error: Bellek silme hatası

### Kullanıcı Talimatları

#### Talimatları Getirme

**Endpoint**: `/user-instructions/{userId}`  
**Metod**: GET  
**Açıklama**: Kullanıcının tüm talimatlarını getirir

**URL Parametreleri**:
| İsim | Gereklilik | Açıklama |
|------|------------|----------|
| userId | Zorunlu | Talimatları getirilecek kullanıcının ID'si |

**Cevap Örneği - Başarılı (200 OK)**:

```json
{
  "id": "user123",
  "userId": "user123",
  "created": "2023-09-10T08:15:30",
  "lastUpdated": "2023-10-15T14:30:45.123",
  "instructions": [
    {
      "id": "instr123",
      "userId": "user123",
      "content": "Cevaplarını her zaman kısa ve öz tut",
      "category": "COMMUNICATION_STYLE",
      "priority": 5,
      "active": true,
      "createdAt": "2023-09-10T08:15:30",
      "updatedAt": "2023-10-15T14:30:45.123"
    },
    {
      "id": "instr124",
      "userId": "user123",
      "content": "Örnekler vererek açıkla",
      "category": "EXPLANATION_STYLE",
      "priority": 3,
      "active": true,
      "createdAt": "2023-09-15T10:20:30",
      "updatedAt": "2023-09-15T10:20:30"
    }
  ]
}
```

**Olası Hatalar**:

- 403 Forbidden: Firebase yetkilendirme hatası
- 500 Internal Server Error: Talimat alma hatası

#### Talimat Ekleme

**Endpoint**: `/user-instructions/{userId}`  
**Metod**: POST  
**Açıklama**: Kullanıcı için yeni bir talimat ekler

**URL Parametreleri**:
| İsim | Gereklilik | Açıklama |
|------|------------|----------|
| userId | Zorunlu | Talimat eklenecek kullanıcının ID'si |

**İstek Gövdesi**:

```json
{
  "content": "Teknik terimleri basit bir dille açıkla",
  "category": "EXPLANATION_STYLE",
  "priority": 4,
  "active": true
}
```

**Cevap Örneği - Başarılı (201 Created)**:

```json
{
  "id": "instr125",
  "userId": "user123",
  "content": "Teknik terimleri basit bir dille açıkla",
  "category": "EXPLANATION_STYLE",
  "priority": 4,
  "active": true,
  "createdAt": "2023-10-18T15:45:30",
  "updatedAt": "2023-10-18T15:45:30"
}
```

**Olası Hatalar**:

- 400 Bad Request: Geçersiz istek verisi
- 403 Forbidden: Firebase yetkilendirme hatası
- 500 Internal Server Error: Talimat ekleme hatası

#### Talimat Güncelleme

**Endpoint**: `/user-instructions/{userId}/{instructionId}`  
**Metod**: PUT  
**Açıklama**: Mevcut bir kullanıcı talimatını günceller

**URL Parametreleri**:
| İsim | Gereklilik | Açıklama |
|------|------------|----------|
| userId | Zorunlu | Talimat güncellenecek kullanıcının ID'si |
| instructionId | Zorunlu | Güncellenecek talimatın ID'si |

**İstek Gövdesi**:

```json
{
  "content": "Teknik terimleri basit bir dille ve örneklerle açıkla",
  "category": "EXPLANATION_STYLE",
  "priority": 5,
  "active": true
}
```

**Cevap Örneği - Başarılı (200 OK)**:

```json
{
  "id": "instr125",
  "userId": "user123",
  "content": "Teknik terimleri basit bir dille ve örneklerle açıkla",
  "category": "EXPLANATION_STYLE",
  "priority": 5,
  "active": true,
  "createdAt": "2023-10-18T15:45:30",
  "updatedAt": "2023-10-18T16:30:15"
}
```

**Olası Hatalar**:

- 400 Bad Request: Geçersiz istek verisi
- 403 Forbidden: Firebase yetkilendirme hatası
- 404 Not Found: Belirtilen talimat bulunamadı
- 500 Internal Server Error: Talimat güncelleme hatası

#### Belirli Talimatı Silme

**Endpoint**: `/user-instructions/{userId}/{instructionId}`  
**Metod**: DELETE  
**Açıklama**: Belirli bir kullanıcı talimatını siler

**URL Parametreleri**:
| İsim | Gereklilik | Açıklama |
|------|------------|----------|
| userId | Zorunlu | Talimat silinecek kullanıcının ID'si |
| instructionId | Zorunlu | Silinecek talimatın ID'si |

**Cevap Örneği - Başarılı (200 OK)**:

```json
{
  "message": "Instruction deleted successfully",
  "success": true
}
```

**Olası Hatalar**:

- 403 Forbidden: Firebase yetkilendirme hatası
- 404 Not Found: Belirtilen talimat bulunamadı
- 500 Internal Server Error: Talimat silme hatası

#### Tüm Talimatları Silme

**Endpoint**: `/user-instructions/{userId}`  
**Metod**: DELETE  
**Açıklama**: Kullanıcının tüm talimatlarını siler

**URL Parametreleri**:
| İsim | Gereklilik | Açıklama |
|------|------------|----------|
| userId | Zorunlu | Tüm talimatları silinecek kullanıcının ID'si |

**Cevap Örneği - Başarılı (200 OK)**:

```json
{
  "message": "All instructions deleted successfully",
  "success": true
}
```

**Olası Hatalar**:

- 403 Forbidden: Firebase yetkilendirme hatası
- 500 Internal Server Error: Talimat silme hatası

### Kullanıcı Bağlamı

#### Bağlam Bilgisini Getirme

**Endpoint**: `/user-context/{userId}`  
**Metod**: GET  
**Açıklama**: Kullanıcının bağlam bilgisini getirir

**URL Parametreleri**:
| İsim | Gereklilik | Açıklama |
|------|------------|----------|
| userId | Zorunlu | Bağlam bilgisi getirilecek kullanıcının ID'si |

**Cevap Örneği - Başarılı (200 OK)**:

```json
{
  "userId": "user123",
  "contextData": {
    "interests": ["Programlama", "Müzik", "Doğa Yürüyüşleri"],
    "expertise": ["Java", "Python", "AI"],
    "preferences": {
      "detailed_explanations": true,
      "code_examples": true
    }
  },
  "createdAt": "2023-09-10T08:15:30",
  "updatedAt": "2023-10-15T14:30:45.123"
}
```

**Olası Hatalar**:

- 403 Forbidden: Firebase yetkilendirme hatası
- 404 Not Found: Kullanıcı bağlamı bulunamadı
- 500 Internal Server Error: Bağlam alma hatası

### Dil Stili Tercihleri

#### Dil Stili Tercihlerini Getirme

**Endpoint**: `/user-preferences/{userId}/language-style`  
**Metod**: GET  
**Açıklama**: Kullanıcının dil stili tercihlerini getirir

**URL Parametreleri**:
| İsim | Gereklilik | Açıklama |
|------|------------|----------|
| userId | Zorunlu | Dil stili tercihleri getirilecek kullanıcının ID'si |

**Cevap Örneği - Başarılı (200 OK)**:

```json
{
  "userId": "user123",
  "formality": "CASUAL",
  "verbosity": "CONCISE",
  "tone": "FRIENDLY",
  "language": "tr",
  "customStyles": ["Mizahi", "Bilgilendirici"],
  "createdAt": "2023-09-10T08:15:30",
  "updatedAt": "2023-10-15T14:30:45.123"
}
```

**Olası Hatalar**:

- 403 Forbidden: Firebase yetkilendirme hatası
- 404 Not Found: Kullanıcı dil stili tercihleri bulunamadı
- 500 Internal Server Error: Tercih alma hatası

#### Dil Stili Tercihlerini Güncelleme

**Endpoint**: `/user-preferences/{userId}/language-style`  
**Metod**: PUT  
**Açıklama**: Kullanıcının dil stili tercihlerini günceller

**URL Parametreleri**:
| İsim | Gereklilik | Açıklama |
|------|------------|----------|
| userId | Zorunlu | Dil stili tercihleri güncellenecek kullanıcının ID'si |

**İstek Gövdesi**:

```json
{
  "formality": "CASUAL",
  "verbosity": "DETAILED",
  "tone": "FRIENDLY",
  "language": "tr",
  "customStyles": ["Mizahi", "Bilgilendirici", "Örnekli"]
}
```

**Cevap Örneği - Başarılı (200 OK)**:

```json
{
  "userId": "user123",
  "formality": "CASUAL",
  "verbosity": "DETAILED",
  "tone": "FRIENDLY",
  "language": "tr",
  "customStyles": ["Mizahi", "Bilgilendirici", "Örnekli"],
  "createdAt": "2023-09-10T08:15:30",
  "updatedAt": "2023-10-18T16:30:15"
}
```

**Olası Hatalar**:

- 400 Bad Request: Geçersiz istek verisi
- 403 Forbidden: Firebase yetkilendirme hatası
- 500 Internal Server Error: Tercih güncelleme hatası

#### Dil Stili Tercihlerini Silme

**Endpoint**: `/user-preferences/{userId}/language-style`  
**Metod**: DELETE  
**Açıklama**: Kullanıcının dil stili tercihlerini siler

**URL Parametreleri**:
| İsim | Gereklilik | Açıklama |
|------|------------|----------|
| userId | Zorunlu | Dil stili tercihleri silinecek kullanıcının ID'si |

**Cevap Örneği - Başarılı (200 OK)**:

```json
{
  "message": "Language style preferences deleted successfully",
  "success": true
}
```

**Olası Hatalar**:

- 403 Forbidden: Firebase yetkilendirme hatası
- 500 Internal Server Error: Tercih silme hatası

## Veri Modelleri

### MemoryEntryRequest

```java
{
  "content": String,       // Bellek girdisinin içeriği (zorunlu)
  "source": String,        // Bilginin kaynağı (ör: "AI analizi", "Kullanıcı ile sohbet")
  "context": String,       // Bilginin elde edildiği bağlam
  "metadata": Map<String, Object>, // Ek metadata bilgileri (isteğe bağlı)
  "timestamp": LocalDateTime, // Oluşturulma zamanı (gönderilmezse otomatik oluşturulur)
  "importance": Double     // Girişin önem derecesi (0.0-1.0, gönderilmezse 1.0 varsayılır)
}
```

### UserMemory

```java
{
  "id": String,           // Kullanıcı ID'si (Firestore doküman ID'si olarak kullanılır)
  "userId": String,       // Kullanıcı ID'si
  "created": LocalDateTime, // Bellek oluşturulma zamanı
  "lastUpdated": LocalDateTime, // Son güncelleme zamanı
  "entries": List<Map<String, Object>> // Bellek girdileri listesi
}
```

### UserInstruction

```java
{
  "id": String,           // Talimat ID'si
  "userId": String,       // Kullanıcı ID'si
  "content": String,      // Talimat içeriği
  "category": String,     // Talimat kategorisi (ör: "COMMUNICATION_STYLE", "EXPLANATION_STYLE")
  "priority": Integer,    // Öncelik (1-10 arası)
  "active": Boolean,      // Talimatın aktif olup olmadığı
  "createdAt": LocalDateTime, // Oluşturulma zamanı
  "updatedAt": LocalDateTime  // Son güncelleme zamanı
}
```

### UserContext

```java
{
  "userId": String,       // Kullanıcı ID'si
  "contextData": Map<String, Object>, // Bağlam verileri
  "createdAt": LocalDateTime, // Oluşturulma zamanı
  "updatedAt": LocalDateTime  // Son güncelleme zamanı
}
```

### LanguageStylePreference

```java
{
  "userId": String,       // Kullanıcı ID'si
  "formality": String,    // Resmiyet (FORMAL, CASUAL, NEUTRAL)
  "verbosity": String,    // Ayrıntı seviyesi (CONCISE, DETAILED, BALANCED)
  "tone": String,         // Ton (FRIENDLY, PROFESSIONAL, ENTHUSIASTIC, NEUTRAL)
  "language": String,     // Dil kodu (tr, en, vb.)
  "customStyles": List<String>, // Özel stil tercihleri
  "createdAt": LocalDateTime, // Oluşturulma zamanı
  "updatedAt": LocalDateTime  // Son güncelleme zamanı
}
```

### MemoryResponse

```java
{
  "message": String,      // İşlem sonucu mesajı
  "success": boolean      // İşlemin başarılı olup olmadığı
}
```

### ErrorResponse

```java
{
  "code": String,         // Hata kodu
  "message": String,      // Hata açıklaması
  "status": int           // HTTP durum kodu
}
```

## Hata Kodları

| Kod                     | Açıklama                                                                       |
| ----------------------- | ------------------------------------------------------------------------------ |
| firebase_auth_error     | Firebase yetkilendirme hatası. Servis hesabı izinleri kontrol edilmelidir.     |
| memory_processing_error | Bellek işleme hatası. Genellikle servis tarafında bir sorun olduğunu gösterir. |
| memory_retrieval_error  | Kullanıcı belleği alınırken hata oluştu.                                       |
| index_out_of_bounds     | Belirtilen indeks geçerli değil.                                               |
| memory_deletion_error   | Bellek girdisi silinirken bir hata oluştu.                                     |
| instruction_error       | Talimat işleme sırasında bir hata oluştu.                                      |
| context_error           | Bağlam bilgisi işleme sırasında bir hata oluştu.                               |
| preference_error        | Tercih işleme sırasında bir hata oluştu.                                       |

## Örnek İstek ve Yanıtlar

### Bellek Girdisi Ekleme Örneği

**İstek:**

```
POST /memories/entries
Header: X-User-Id: user123
Content-Type: application/json

{
  "content": "Kullanıcı yazılım geliştirme ile ilgileniyor",
  "source": "Kullanıcı profili",
  "context": "Hobi ve ilgi alanları",
  "metadata": {
    "confidence": 0.95,
    "category": "kariyer"
  },
  "importance": 0.8
}
```

**Yanıt:**

```
HTTP/1.1 200 OK
Content-Type: application/json

{
  "message": "Memory entry added successfully",
  "success": true
}
```

### Kullanıcı Belleği Getirme Örneği

**İstek:**

```
GET /memories/user123
```

**Yanıt:**

```
HTTP/1.1 200 OK
Content-Type: application/json

{
  "id": "user123",
  "userId": "user123",
  "created": "2023-09-10T08:15:30",
  "lastUpdated": "2023-10-17T09:22:15",
  "entries": [
    {
      "content": "Kullanıcı yazılım geliştirme ile ilgileniyor",
      "source": "Kullanıcı profili",
      "context": "Hobi ve ilgi alanları",
      "metadata": {
        "confidence": 0.95,
        "category": "kariyer"
      },
      "timestamp": "2023-10-17T09:22:15",
      "importance": 0.8
    }
  ]
}
```

### Firebase Yetkilendirme Hatası Örneği

**İstek:**

```
GET /memories/user123
```

**Yanıt:**

```
HTTP/1.1 403 Forbidden
Content-Type: application/json

{
  "code": "firebase_auth_error",
  "message": "Firebase yetkilendirme hatası. Servis hesabı izinlerini kontrol edin.",
  "status": 403
}
```

Bu dokümantasyon, User Memory Service'in temel API entegrasyonu için gereken tüm bilgileri içermektedir. Herhangi bir soru veya sorun durumunda, lütfen backend ekibi ile iletişime geçiniz.
