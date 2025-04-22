# Notification Service - Frontend Entegrasyon Kılavuzu

## API Endpoint Bilgileri

- **Base URL**: `/api/notifications` (API Gateway üzerinden)
- **Port**: 8053 (Doğrudan servis erişimi için)

## Gerekli Headers

Tüm API isteklerinde aşağıdaki header'lar sağlanmalıdır:

```
X-User-Id: {kullanıcı-id}
X-User-Role:
X-User-Email:
Content-Type: application/json
```

## Bildirim İşlemleri

### 1. Kullanıcı Bildirimlerini Getirme

Bir kullanıcının tüm bildirimlerini veya sadece okunmamış bildirimlerini getirmek için:

```javascript
// Tüm bildirimleri getir
GET /notifications/user/{userId}

// Sadece okunmamış bildirimleri getir
GET /notifications/user/{userId}?onlyUnread=true
```

**Yanıt Formatı:**

```json
[
  {
    "id": "bildirim-id-1",
    "userId": "user123",
    "title": "Yeni mesaj",
    "content": "Projede yeni bir yorum var",
    "type": "IN_APP",
    "read": false,
    "createdAt": "2023-10-01T12:00:00"
  }
]
```

### 2. Bildirimi Okundu Olarak İşaretleme

```javascript
PUT / notifications / { id } / read;
```

**Yanıt:**

```json
{
  "id": "bildirim-id-1",
  "userId": "user123",
  "title": "Yeni mesaj",
  "content": "Projede yeni bir yorum var",
  "type": "IN_APP",
  "read": true,
  "createdAt": "2023-10-01T12:00:00"
}
```

### 3. Bildirimi Silme

```javascript
DELETE / notifications / { id };
```

**Yanıt:** HTTP 204 (No Content)

## Kullanıcı Tercihleri Yönetimi

Bildirim tercihleri, merkezi User Service tarafından yönetilen kullanıcı tercihlerinin bir parçasıdır. Bu yapı sayesinde tüm kullanıcı tercihleri tek bir yerde toplanmış olur.

### 1. Kullanıcı Tercihlerini Getirme

```javascript
GET / users / { userId } / preferences;
```

**Yanıt:**

```json
{
  "userId": "user123",
  "theme": "system",
  "themeSchema": "default",
  "language": "tr",
  "layout": "collapsibleSide",
  "notifications": {
    "general": true,
    "comments": true,
    "mentions": true
  },
  "pushEnabled": true,
  "aiModelFavorites": ["gpt-4", "claude-3"],
  "createdAt": 1681234567890,
  "updatedAt": 1682345678901
}
```

### 2. Bildirim Tercihlerini Güncelleme

Bildirim tercihleri, tüm kullanıcı tercihleri içerisinde `notifications` ve `pushEnabled` alanları ile yönetilir:

```javascript
PUT / users / { userId } / preferences;
```

**İstek Gövdesi:**

```json
{
  "notifications": {
    "general": true,
    "comments": false,
    "mentions": true
  },
  "pushEnabled": true
}
```

### 3. Push Bildirimleri İçin Device Token Güncelleme

```javascript
PUT / users / { userId } / preferences;
```

**İstek Gövdesi:**

```json
{
  "deviceToken": "yeni-firebase-device-token"
}
```

> **Not:** Bu entegrasyon, tüm kullanıcı tercihlerinin User Service üzerinden tek bir yerden yönetilmesini sağlayarak kod tekrarını önler ve tutarlılığı artırır.

## Frontend Entegrasyon Kodları

### React ile Bildirimleri Görüntüleme

```javascript
import React, { useState, useEffect } from "react";
import axios from "axios";

function NotificationList({ userId }) {
  const [notifications, setNotifications] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchNotifications = async () => {
      try {
        setLoading(true);
        const response = await axios.get(`/api/notifications/user/${userId}`);
        setNotifications(response.data);
      } catch (error) {
        console.error("Bildirimler alınamadı:", error);
      } finally {
        setLoading(false);
      }
    };

    fetchNotifications();
    // Her 30 saniyede bir yenile
    const intervalId = setInterval(fetchNotifications, 30000);

    return () => clearInterval(intervalId);
  }, [userId]);

  const markAsRead = async (notificationId) => {
    try {
      await axios.put(`/api/notifications/${notificationId}/read`);
      setNotifications((currentNotifications) =>
        currentNotifications.map((notification) =>
          notification.id === notificationId
            ? { ...notification, read: true }
            : notification
        )
      );
    } catch (error) {
      console.error("Bildirim okundu işaretlenemedi:", error);
    }
  };

  const deleteNotification = async (notificationId) => {
    try {
      await axios.delete(`/api/notifications/${notificationId}`);
      setNotifications((currentNotifications) =>
        currentNotifications.filter(
          (notification) => notification.id !== notificationId
        )
      );
    } catch (error) {
      console.error("Bildirim silinemedi:", error);
    }
  };

  if (loading) return <div>Bildirimler yükleniyor...</div>;

  return (
    <div className="notification-list">
      {notifications.length === 0 ? (
        <p>Bildiriminiz bulunmuyor.</p>
      ) : (
        notifications.map((notification) => (
          <div
            key={notification.id}
            className={`notification-item ${
              notification.read ? "read" : "unread"
            }`}
          >
            <h4>{notification.title}</h4>
            <p>{notification.content}</p>
            <div className="notification-actions">
              {!notification.read && (
                <button onClick={() => markAsRead(notification.id)}>
                  Okundu İşaretle
                </button>
              )}
              <button onClick={() => deleteNotification(notification.id)}>
                Sil
              </button>
            </div>
          </div>
        ))
      )}
    </div>
  );
}

export default NotificationList;
```

### Bildirim Sayacı (Badge)

```javascript
import React, { useState, useEffect } from "react";
import axios from "axios";

function NotificationBadge({ userId }) {
  const [count, setCount] = useState(0);

  useEffect(() => {
    const fetchUnreadCount = async () => {
      try {
        const response = await axios.get(
          `/api/notifications/user/${userId}?onlyUnread=true`
        );
        setCount(response.data.length);
      } catch (error) {
        console.error("Okunmamış bildirim sayısı alınamadı:", error);
      }
    };

    fetchUnreadCount();
    const intervalId = setInterval(fetchUnreadCount, 60000); // Her dakika güncelle

    return () => clearInterval(intervalId);
  }, [userId]);

  return count > 0 ? <span className="notification-badge">{count}</span> : null;
}

export default NotificationBadge;
```

### Push Bildirimleri için Firebase Entegrasyonu

```javascript
import { initializeApp } from "firebase/app";
import { getMessaging, getToken, onMessage } from "firebase/messaging";
import axios from "axios";

// Firebase yapılandırması
const firebaseConfig = {
  // Firebase console'dan alınan yapılandırma
  apiKey: "YOUR_API_KEY",
  authDomain: "your-app.firebaseapp.com",
  projectId: "your-project-id",
  messagingSenderId: "your-sender-id",
  appId: "your-app-id",
};

// Firebase başlatma
const app = initializeApp(firebaseConfig);
const messaging = getMessaging(app);

// Push bildirimleri için token alıp kaydetme
export async function setupPushNotifications(userId) {
  try {
    // Tarayıcıda bildirim izni iste
    const permission = await Notification.requestPermission();

    if (permission === "granted") {
      // FCM token al
      const token = await getToken(messaging, {
        vapidKey: "YOUR_VAPID_KEY",
      });

      if (token) {
        // Token'ı kullanıcı tercihlerine kaydet (User Service API)
        await axios.put(`/api/users/${userId}/preferences`, {
          deviceToken: token,
        });

        console.log("Push bildirimi için token kaydedildi");

        // Ön planda gelen bildirimleri dinle
        onMessage(messaging, (payload) => {
          console.log("Bildirim alındı:", payload);

          // Bildirimi göster
          const notificationTitle = payload.notification.title;
          const notificationOptions = {
            body: payload.notification.body,
            icon: "/notification-icon.png",
          };

          new Notification(notificationTitle, notificationOptions);
        });
      }
    }
  } catch (error) {
    console.error("Push bildirimi kurulumunda hata:", error);
  }
}
```

## Hata İşleme

- **400**: Geçersiz istek (eksik veya yanlış parametreler)
- **401**: Yetkilendirme hatası (geçersiz token veya eksik header)
- **404**: Kaynak bulunamadı (bildirim veya tercih)
- **500**: Sunucu hatası

## İyi Uygulama Önerileri

1. **Optimizasyon**:

   - Tüm bildirimleri her seferinde tekrar çekmek yerine, WebSocket veya SSE kullanarak gerçek zamanlı bildirimler alın.
   - Bildirimleri sayfalı olarak yükleyin (10 veya 20 adet).

2. **Kullanıcı Deneyimi**:

   - Yeni bildirimler alındığında ses veya titreşim ile kullanıcıyı uyarın.
   - Bildirimler için "hepsini okundu olarak işaretle" seçeneği ekleyin.

3. **Hata İşleme**:

   - API isteklerinde retry mekanizması kullanın.
   - Kullanıcıya, hata durumunda anlamlı mesajlar gösterin.

4. **Offline Kullanım**:
   - Önceden alınmış bildirimleri `localStorage` veya `IndexedDB`'de veya SWR'de saklayarak offline erişim sağlayın.
