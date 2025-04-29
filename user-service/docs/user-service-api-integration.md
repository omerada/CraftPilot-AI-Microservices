# User Service API Entegrasyon Rehberi

Bu dokümantasyon, frontend geliştiricilerinin Craft-Pilot-AI platformunda User Service API entegrasyonu için gerekli tüm bilgileri içerir.

## İçindekiler

1. [Genel Bilgiler](#genel-bilgiler)
2. [Kimlik Doğrulama](#kimlik-doğrulama)
3. [Kullanıcı İşlemleri](#kullanıcı-i̇şlemleri)
4. [Kullanıcı Tercihleri](#kullanıcı-tercihleri)
5. [AI Model İşlemleri](#ai-model-i̇şlemleri)
6. [Hata Kodları ve Mesajlar](#hata-kodları-ve-mesajlar)
7. [Örnek Kullanım](#örnek-kullanım)

## Genel Bilgiler

- **Base URL**: `https://api.craftpilot.ai/user-service`
- **Content-Type**: `application/json`
- **Karakter Seti**: UTF-8

## Kimlik Doğrulama

Tüm istekler, Firebase Authentication tarafından üretilen JWT token ile yetkilendirilmelidir:

```
Authorization: Bearer {firebase_jwt_token}
```

Bazı isteklerde Firebase token doğrudan header olarak gönderilir:

```
Firebase-Token: {firebase_token}
```

## Kullanıcı İşlemleri

### 1. Yeni Kullanıcı Oluşturma

Firebase token kullanarak yeni bir kullanıcı oluşturur ve benzersiz bir kullanıcı adı atar.

- **Endpoint**: `POST /users`
- **Headers**:
  - `Firebase-Token: {firebase_token}` (Zorunlu)
- **Başarılı Yanıt**: `HTTP 201 Created`

```json
{
  "id": "firebase_user_id",
  "email": "user@example.com",
  "username": "username",
  "displayName": "Tam Ad",
  "photoUrl": "https://example.com/photo.jpg",
  "role": "USER",
  "status": "ACTIVE",
  "createdAt": 1682541258000,
  "updatedAt": 1682541258000
}
```

### 2. Kullanıcı Bilgilerini Getirme

- **Endpoint**: `GET /users/{id}`
- **Path Parametreleri**:
  - `id`: Kullanıcı ID
- **Başarılı Yanıt**: `HTTP 200 OK`

```json
{
  "id": "firebase_user_id",
  "email": "user@example.com",
  "username": "username",
  "displayName": "Tam Ad",
  "photoUrl": "https://example.com/photo.jpg",
  "role": "USER",
  "status": "ACTIVE",
  "createdAt": 1682541258000,
  "updatedAt": 1682541258000
}
```

### 3. Kullanıcı Bilgilerini Güncelleme

- **Endpoint**: `PUT /users/{id}`
- **Path Parametreleri**:
  - `id`: Kullanıcı ID
- **Request Body**:

```json
{
  "username": "yeni_username",
  "displayName": "Yeni Ad",
  "photoUrl": "https://example.com/new_photo.jpg"
}
```

- **Başarılı Yanıt**: `HTTP 200 OK`

```json
{
  "id": "firebase_user_id",
  "email": "user@example.com",
  "username": "yeni_username",
  "displayName": "Yeni Ad",
  "photoUrl": "https://example.com/new_photo.jpg",
  "role": "USER",
  "status": "ACTIVE",
  "createdAt": 1682541258000,
  "updatedAt": 1682541300000
}
```

### 4. Kullanıcı Silme

- **Endpoint**: `DELETE /users/{id}`
- **Path Parametreleri**:
  - `id`: Kullanıcı ID
- **Başarılı Yanıt**: `HTTP 204 No Content`

### 5. Kullanıcı Durumunu Güncelleme

- **Endpoint**: `PATCH /users/{id}/status`
- **Path Parametreleri**:
  - `id`: Kullanıcı ID
- **Query Parametreleri**:
  - `status`: Yeni durum (`ACTIVE`, `INACTIVE`, `SUSPENDED`, `DELETED`)
- **Başarılı Yanıt**: `HTTP 200 OK`

```json
{
  "id": "firebase_user_id",
  "email": "user@example.com",
  "username": "username",
  "displayName": "Tam Ad",
  "photoUrl": "https://example.com/photo.jpg",
  "role": "USER",
  "status": "INACTIVE",
  "createdAt": 1682541258000,
  "updatedAt": 1682541400000
}
```

### 6. Kullanıcı Arama

Email veya kullanıcı adına göre kullanıcı arar.

- **Endpoint**: `GET /users/search`
- **Query Parametreleri** (en az biri zorunlu):
  - `email`: E-posta adresi
  - `username`: Kullanıcı adı
- **Başarılı Yanıt**: `HTTP 200 OK`

```json
{
  "id": "firebase_user_id",
  "email": "user@example.com",
  "username": "username",
  "displayName": "Tam Ad",
  "photoUrl": "https://example.com/photo.jpg",
  "role": "USER",
  "status": "ACTIVE",
  "createdAt": 1682541258000,
  "updatedAt": 1682541258000
}
```

### 7. Firebase Kullanıcısını Senkronize Etme

Firebase Authentication tarafından güncellenmiş kullanıcı bilgilerini sistemle senkronize eder.

- **Endpoint**: `POST /users/sync`
- **Headers**:
  - `Firebase-Token: {firebase_token}` (Zorunlu)
- **Başarılı Yanıt**: `HTTP 200 OK`

```json
{
  "id": "firebase_user_id",
  "email": "user@example.com",
  "username": "username",
  "displayName": "Tam Ad",
  "photoUrl": "https://example.com/photo.jpg",
  "role": "USER",
  "status": "ACTIVE",
  "createdAt": 1682541258000,
  "updatedAt": 1682541500000
}
```

### 8. Firebase Güncellemelerini Senkronize Etme

Firebase'de yapılan değişiklikleri sisteme aktarır.

- **Endpoint**: `PUT /users/{id}/firebase-sync`
- **Path Parametreleri**:
  - `id`: Kullanıcı ID
- **Request Body**: Güncellenen kullanıcı verileri
- **Başarılı Yanıt**: `HTTP 200 OK`

## Kullanıcı Tercihleri

### 1. Kullanıcı Tercihlerini Getirme

- **Endpoint**: `GET /users/{userId}/preferences`
- **Path Parametreleri**:
  - `userId`: Kullanıcı ID
- **Başarılı Yanıt**: `HTTP 200 OK`

```json
{
  "userId": "firebase_user_id",
  "theme": "dark",
  "language": "tr",
  "themeSchema": "default",
  "layout": "compact",
  "notifications": {
    "general": true,
    "news": false
  },
  "pushEnabled": true,
  "aiModelFavorites": ["gpt-4", "claude-v2"],
  "lastSelectedModelId": "gpt-4",
  "createdAt": 1682541258000,
  "updatedAt": 1682541258000
}
```

### 2. Kullanıcı Tercihleri Oluşturma

- **Endpoint**: `POST /users/{userId}/preferences`
- **Path Parametreleri**:
  - `userId`: Kullanıcı ID
- **Request Body**:

```json
{
  "theme": "dark",
  "language": "tr",
  "layout": "compact",
  "aiModelFavorites": ["gpt-4", "claude-v2"],
  "pushEnabled": true
}
```

- **Başarılı Yanıt**: `HTTP 201 Created`

```json
{
  "userId": "firebase_user_id",
  "theme": "dark",
  "language": "tr",
  "themeSchema": "default",
  "layout": "compact",
  "notifications": {
    "general": true
  },
  "pushEnabled": true,
  "aiModelFavorites": ["gpt-4", "claude-v2"],
  "lastSelectedModelId": null,
  "createdAt": 1682541600000,
  "updatedAt": 1682541600000
}
```

### 3. Kullanıcı Tercihlerini Güncelleme

- **Endpoint**: `PUT /users/{userId}/preferences`
- **Path Parametreleri**:
  - `userId`: Kullanıcı ID
- **Request Body**:

```json
{
  "theme": "light",
  "language": "en",
  "notifications": {
    "general": true,
    "news": true
  }
}
```

- **Başarılı Yanıt**: `HTTP 200 OK`

```json
{
  "userId": "firebase_user_id",
  "theme": "light",
  "language": "en",
  "themeSchema": "default",
  "layout": "compact",
  "notifications": {
    "general": true,
    "news": true
  },
  "pushEnabled": true,
  "aiModelFavorites": ["gpt-4", "claude-v2"],
  "lastSelectedModelId": null,
  "createdAt": 1682541600000,
  "updatedAt": 1682541700000
}
```

### 4. Kullanıcı Tercihlerini Silme

- **Endpoint**: `DELETE /users/{userId}/preferences`
- **Path Parametreleri**:
  - `userId`: Kullanıcı ID
- **Başarılı Yanıt**: `HTTP 204 No Content`

### 5. Tema Tercihini Güncelleme

- **Endpoint**: `PUT /users/{userId}/preferences/theme`
- **Path Parametreleri**:
  - `userId`: Kullanıcı ID
- **Request Body**:

```json
"light"
```

- **Başarılı Yanıt**: `HTTP 200 OK`

```json
{
  "userId": "firebase_user_id",
  "theme": "light",
  "language": "en",
  "themeSchema": "default",
  "layout": "compact",
  "notifications": {
    "general": true,
    "news": true
  },
  "pushEnabled": true,
  "aiModelFavorites": ["gpt-4", "claude-v2"],
  "lastSelectedModelId": null,
  "createdAt": 1682541600000,
  "updatedAt": 1682541800000
}
```

### 6. Favori AI Modellerini Güncelleme

- **Endpoint**: `PUT /users/{userId}/preferences/ai-model-favorites`
- **Path Parametreleri**:
  - `userId`: Kullanıcı ID
- **Request Body**:

```json
["gpt-4", "claude-v2", "gemini-pro"]
```

- **Başarılı Yanıt**: `HTTP 200 OK`

```json
{
  "userId": "firebase_user_id",
  "theme": "light",
  "language": "en",
  "themeSchema": "default",
  "layout": "compact",
  "notifications": {
    "general": true,
    "news": true
  },
  "pushEnabled": true,
  "aiModelFavorites": ["gpt-4", "claude-v2", "gemini-pro"],
  "lastSelectedModelId": null,
  "createdAt": 1682541600000,
  "updatedAt": 1682541900000
}
```

### 7. Favori AI Modellerini Getirme

- **Endpoint**: `GET /users/{userId}/preferences/ai-model-favorites`
- **Path Parametreleri**:
  - `userId`: Kullanıcı ID
- **Başarılı Yanıt**: `HTTP 200 OK`

```json
["gpt-4", "claude-v2", "gemini-pro"]
```

## AI Model İşlemleri

### 1. Kullanıcıya Uygun AI Modellerini Getirme

- **Endpoint**: `GET /users/models/available`
- **Başarılı Yanıt**: `HTTP 200 OK`

```json
{
  "models": [
    {
      "id": "google/gemini-2.0-flash-lite-001",
      "name": "Gemini Flash",
      "provider": "google",
      "description": "Hızlı yanıt veren çok amaçlı model",
      "contextLength": 30000,
      "maxInputTokens": 6000,
      "category": "basic",
      "creditCost": 5,
      "creditType": "STANDARD",
      "popular": true
    },
    {
      "id": "anthropic/claude-3-sonnet",
      "name": "Claude 3 Sonnet",
      "provider": "anthropic",
      "description": "Dengeli performans ve hassasiyet sunan model",
      "contextLength": 180000,
      "maxInputTokens": 6000,
      "category": "premium",
      "creditCost": 25,
      "creditType": "ADVANCED",
      "popular": true
    }
  ],
  "providers": [
    {
      "id": "google",
      "name": "Google",
      "displayName": "Google AI",
      "icon": "google-icon",
      "description": "Google tarafından geliştirilen AI modelleri"
    },
    {
      "id": "anthropic",
      "name": "Anthropic",
      "displayName": "Anthropic AI",
      "icon": "anthropic-icon",
      "description": "Anthropic tarafından geliştirilen Claude AI modelleri"
    }
  ]
}
```

## Hata Kodları ve Mesajlar

| HTTP Kodu | Hata Mesajı                           | Açıklama                    |
| --------- | ------------------------------------- | --------------------------- |
| 400       | "Invalid request data"                | Geçersiz istek verisi       |
| 401       | "Unauthorized"                        | Yetkilendirme hatası        |
| 403       | "Forbidden"                           | İzin hatası                 |
| 404       | "User not found with id: {id}"        | Kullanıcı bulunamadı        |
| 409       | "This username is already taken"      | Kullanıcı adı zaten alınmış |
| 422       | "Validation failed: {field}: {error}" | Doğrulama hatası            |
| 500       | "Internal server error"               | Sunucu hatası               |
| 503       | "Service unavailable"                 | Servis kullanılamıyor       |

## Örnek Kullanım

### JavaScript/TypeScript ile Kullanım Örnekleri

#### 1. Kullanıcı Bilgilerini Getirme

```typescript
async function getUserDetails(userId: string, token: string) {
  try {
    const response = await fetch(
      `https://api.craftpilot.ai/user-service/users/${userId}`,
      {
        headers: {
          Authorization: `Bearer ${token}`,
          "Content-Type": "application/json",
        },
      }
    );

    if (!response.ok) {
      throw new Error(`Hata: ${response.status}`);
    }

    return await response.json();
  } catch (error) {
    console.error("Kullanıcı bilgileri alınamadı:", error);
    throw error;
  }
}
```

#### 2. Kullanıcı Tercihlerini Güncelleme

```typescript
async function updateUserPreferences(
  userId: string,
  token: string,
  preferences: object
) {
  try {
    const response = await fetch(
      `https://api.craftpilot.ai/user-service/users/${userId}/preferences`,
      {
        method: "PUT",
        headers: {
          Authorization: `Bearer ${token}`,
          "Content-Type": "application/json",
        },
        body: JSON.stringify(preferences),
      }
    );

    if (!response.ok) {
      throw new Error(`Hata: ${response.status}`);
    }

    return await response.json();
  } catch (error) {
    console.error("Tercihler güncellenemedi:", error);
    throw error;
  }
}
```

#### 3. Favori AI Modellerini Ayarlama

```typescript
async function updateFavoriteModels(
  userId: string,
  token: string,
  favoriteModels: string[]
) {
  try {
    const response = await fetch(
      `https://api.craftpilot.ai/user-service/users/${userId}/preferences/ai-model-favorites`,
      {
        method: "PUT",
        headers: {
          Authorization: `Bearer ${token}`,
          "Content-Type": "application/json",
        },
        body: JSON.stringify(favoriteModels),
      }
    );

    if (!response.ok) {
      throw new Error(`Hata: ${response.status}`);
    }

    return await response.json();
  } catch (error) {
    console.error("Favori modeller güncellenemedi:", error);
    throw error;
  }
}
```

#### 4. Yeni Kullanıcı Oluşturma (Firebase Token İle)

```typescript
async function createNewUser(firebaseToken: string) {
  try {
    const response = await fetch(
      "https://api.craftpilot.ai/user-service/users",
      {
        method: "POST",
        headers: {
          "Firebase-Token": firebaseToken,
          "Content-Type": "application/json",
        },
      }
    );

    if (!response.ok) {
      throw new Error(`Hata: ${response.status}`);
    }

    return await response.json();
  } catch (error) {
    console.error("Kullanıcı oluşturulamadı:", error);
    throw error;
  }
}
```

#### 5. Kullanıcı Durumunu Güncelleme

```typescript
async function updateUserStatus(userId: string, token: string, status: string) {
  try {
    const response = await fetch(
      `https://api.craftpilot.ai/user-service/users/${userId}/status?status=${status}`,
      {
        method: "PATCH",
        headers: {
          Authorization: `Bearer ${token}`,
          "Content-Type": "application/json",
        },
      }
    );

    if (!response.ok) {
      throw new Error(`Hata: ${response.status}`);
    }

    return await response.json();
  } catch (error) {
    console.error("Kullanıcı durumu güncellenemedi:", error);
    throw error;
  }
}
```

Bu dokümantasyonla ilgili herhangi bir sorunuz olursa, backend ekibiyle iletişime geçebilirsiniz.
