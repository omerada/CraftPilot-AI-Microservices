# Activity Log Service - Frontend Entegrasyonu

Bu belge, CraftPilot'un Activity Log servisinin frontend uygulamasında nasıl kullanılacağını açıklamaktadır. Activity Log servisi, sistem içerisindeki kullanıcı aktivitelerinin kaydedilmesini ve görüntülenmesini sağlar.

## API Endpoint Bilgileri

### Aktivite Loglarını Listeleme

**Endpoint:** `/logs`  
**Method:** GET  
**Açıklama:** Kullanıcı aktivitelerini filtreleyerek ve sayfalayarak listeler.

**Gerekli Headers:**

```
X-User-Id: {kullanıcı kimliği}
X-User-Role: {kullanıcı rolü - ADMIN veya USER}
X-User-Email: {kullanıcı e-posta adresi}
```

**Query Parametreleri:**

- `userId` (opsiyonel): Belirli bir kullanıcının loglarını filtrelemek için
- `actionType` (opsiyonel): Belirli bir işlem türüne göre filtreleme
- `fromDate` (opsiyonel): Başlangıç tarihi (ISO-8601 formatında, örn: "2023-01-01T00:00:00Z")
- `toDate` (opsiyonel): Bitiş tarihi (ISO-8601 formatında, örn: "2023-12-31T23:59:59Z")
- `page` (opsiyonel, varsayılan: 0): Sayfa numarası (0'dan başlar)
- `size` (opsiyonel, varsayılan: 20): Sayfa başına log sayısı

**Örnek İstek:**

```
GET /logs?userId=12345&actionType=CONTENT_CREATED&fromDate=2023-01-01T00:00:00Z&toDate=2023-12-31T23:59:59Z&page=0&size=10
```

**Yanıt Formatı:**

```json
{
  "content": [
    {
      "id": "log123",
      "userId": "12345",
      "actionType": "CONTENT_CREATED",
      "timestamp": "2023-06-15T14:22:31Z",
      "metadata": {
        "title": "Yeni İçerik",
        "contentId": "cont789",
        "summary": "Kısa içerik özeti"
      }
    }
    // ... diğer loglar
  ],
  "pageNumber": 0,
  "pageSize": 10,
  "totalElements": 42,
  "totalPages": 5
}
```

## Kullanım Örnekleri

### React ile Entegrasyon

```jsx
import { useState, useEffect } from "react";
import axios from "axios";

function ActivityLogPage() {
  const [logs, setLogs] = useState([]);
  const [loading, setLoading] = useState(false);
  const [filters, setFilters] = useState({
    userId: "",
    actionType: "",
    fromDate: "",
    toDate: "",
    page: 0,
    size: 20,
  });
  const [pagination, setPagination] = useState({
    totalElements: 0,
    totalPages: 0,
  });

  const fetchLogs = async () => {
    setLoading(true);
    try {
      const params = new URLSearchParams();
      Object.entries(filters).forEach(([key, value]) => {
        if (value) params.append(key, value);
      });

      const response = await axios.get(`/logs?${params}`, {
        headers: {
          "X-User-Id": currentUser.id,
          "X-User-Role": currentUser.role,
          "X-User-Email": currentUser.email,
        },
      });

      setLogs(response.data.content);
      setPagination({
        totalElements: response.data.totalElements,
        totalPages: response.data.totalPages,
      });
    } catch (error) {
      console.error("Log verileri alınırken hata oluştu:", error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchLogs();
  }, [filters.page, filters.size]);

  // Log içeriğinden özet çıkarmak için yardımcı fonksiyon
  const getLogSummary = (metadata) => {
    if (!metadata) return "";

    try {
      return (
        metadata.title ||
        metadata.summary ||
        JSON.stringify(metadata).substring(0, 50) + "..."
      );
    } catch (e) {
      return String(metadata).substring(0, 50) + "...";
    }
  };

  return (
    <div>
      {/* Filtreleme formu burada */}

      {loading ? (
        <div>Yükleniyor...</div>
      ) : (
        <div>
          <table>
            <thead>
              <tr>
                <th>Tarih/Saat</th>
                <th>Kullanıcı</th>
                <th>İşlem Türü</th>
                <th>Detay</th>
              </tr>
            </thead>
            <tbody>
              {logs.map((log) => (
                <tr key={log.id}>
                  <td>{new Date(log.timestamp).toLocaleString("tr-TR")}</td>
                  <td>{log.userId}</td>
                  <td>{log.actionType}</td>
                  <td>{getLogSummary(log.metadata)}</td>
                </tr>
              ))}
            </tbody>
          </table>

          {/* Sayfalama kontrolleri */}
          <div>
            <button
              disabled={filters.page === 0}
              onClick={() => setFilters({ ...filters, page: filters.page - 1 })}
            >
              Önceki Sayfa
            </button>

            <span>
              Sayfa {filters.page + 1} / {pagination.totalPages}
            </span>

            <button
              disabled={filters.page >= pagination.totalPages - 1}
              onClick={() => setFilters({ ...filters, page: filters.page + 1 })}
            >
              Sonraki Sayfa
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
```

## Öneriler ve Best Practices

1. **Tarih Formatı:** Backend'e tarih gönderirken ISO-8601 formatını kullanın (YYYY-MM-DDThh:mm:ssZ)
2. **Önbellek:** Sık değişmeyen log verileri için önbellek kullanabilirsiniz
3. **Hata Yönetimi:** API yanıtlarında hata durumları için uygun UI feedback'i sağlayın
4. **Responsive Tasarım:** Mobil cihazlarda log tablosunun okunabilir olmasını sağlayın
5. **Performans:** Büyük log listeleri için virtualized liste kullanmayı düşünün

## Action Type Örnekleri

Activity log servisinde yaygın olarak kullanılan action type değerleri:

- `LOGIN`: Kullanıcı girişi
- `LOGOUT`: Kullanıcı çıkışı
- `CONTENT_CREATED`: İçerik oluşturma
- `CONTENT_UPDATED`: İçerik güncelleme
- `CONTENT_DELETED`: İçerik silme
- `SETTINGS_CHANGED`: Ayar değişikliği
- `USER_CREATED`: Yeni kullanıcı oluşturma
- `PERMISSION_CHANGED`: İzin değişikliği
- `AI_REQUEST`: Yapay zeka isteği
