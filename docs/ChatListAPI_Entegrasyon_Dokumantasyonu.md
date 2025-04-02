# ChatGPT Benzeri Sohbet Listesi API ve Infinite Scrolling Entegrasyon Dokümantasyonu

Bu dokümantasyon, backend'de eklediğimiz yeni API endpoint'ini ve frontend'de kullanılacak sonsuz kaydırma (infinite scrolling) özelliğine sahip sohbet listesi yapısını açıklamaktadır.

## 1. API Endpoint Detayları

### Endpoint Bilgileri

- **URL**: `/ai/chat/flat-histories`
- **Metod**: GET
- **Amaç**: Kullanıcının sohbet geçmişlerini düz bir liste olarak getirir ve chatGPT benzeri pagination yapar

### İstek Parametreleri

| Parametre | Tip    | Zorunluluk   | Varsayılan | Açıklama                                    |
| --------- | ------ | ------------ | ---------- | ------------------------------------------- |
| userId    | string | Zorunlu      | -          | Kullanıcının benzersiz ID'si                |
| offset    | number | İsteğe bağlı | 0          | Kaçıncı öğeden başlanacağı (sayfalama için) |
| limit     | number | İsteğe bağlı | 20         | Kaç öğe getirileceği (sayfalama için)       |
| order     | string | İsteğe bağlı | "updated"  | Sıralama kriteri (updated veya created)     |

### Yanıt Yapısı

```json
{
  "items": [
    {
      "id": "chat-id-1",
      "title": "Sohbet Başlığı",
      "create_time": "2023-05-20T12:34:56.789Z",
      "update_time": "2023-05-21T10:11:12.000Z",
      "is_archived": false,
      "snippet": "Bu sohbetin içeriğinden bir kesit..."
    }
    // ... diğer sohbet öğeleri
  ],
  "total": 420,
  "limit": 20,
  "offset": 0
}
```

| Alan   | Tip    | Açıklama                       |
| ------ | ------ | ------------------------------ |
| items  | array  | Sohbet öğelerinin listesi      |
| total  | number | Toplam sohbet sayısı           |
| limit  | number | İstek başına alınan öğe sayısı |
| offset | number | Kaçıncı öğeden başlandığı      |

#### Sohbet Öğesi (items dizisindeki her öğe)

| Alan        | Tip     | Açıklama                                         |
| ----------- | ------- | ------------------------------------------------ |
| id          | string  | Sohbetin benzersiz ID'si                         |
| title       | string  | Sohbetin başlığı                                 |
| create_time | string  | Oluşturulma zamanı (ISO 8601 formatında)         |
| update_time | string  | Son güncellenme zamanı (ISO 8601 formatında)     |
| is_archived | boolean | Sohbetin arşivlenip arşivlenmediği               |
| snippet     | string  | Sohbetin içeriğinden kısa bir alıntı (opsiyonel) |

## 2. Örnek API Kullanımı

### Örnek İstek

```javascript
// Axios kullanarak örnek istek
axios
  .get("/api/chat/flat-histories", {
    params: {
      userId: "user123",
      offset: 0,
      limit: 20,
      order: "updated",
    },
  })
  .then((response) => {
    console.log(response.data);
  })
  .catch((error) => {
    console.error("Hata:", error);
  });
```

### Örnek Yanıt

```json
{
  "items": [
    {
      "id": "chat-7e10d36d-a2db-49a2-a5f6-ac5901ecc059",
      "title": "ServerWebExchange Bağımlılık Hatası",
      "create_time": "2023-05-17T19:17:04.676910Z",
      "update_time": "2023-05-17T19:19:43.440850Z",
      "is_archived": false,
      "snippet": "ServerWebExchange bağımlılık hatası genellikle Spring WebFlux projelerinde görülür..."
    },
    {
      "id": "chat-67b24ead-4588-800a-8525-4b07e5dfa63b",
      "title": "Unhealthy Docker Container Nedeni",
      "create_time": "2023-05-16T20:46:37.635556Z",
      "update_time": "2023-05-16T20:47:14.045011Z",
      "is_archived": false,
      "snippet": "Docker container'ınız unhealthy durumuna geçmesinin birkaç nedeni olabilir..."
    }
    // ... diğer sohbetler
  ],
  "total": 420,
  "limit": 20,
  "offset": 0
}
```

## 3. Frontend Entegrasyon Kılavuzu

Sağlanan `ChatListInfiniteScroll` komponenti, sohbet listesini kategorize ederek ve sonsuz kaydırma özelliğiyle gösterir. Aşağıda bu komponentin entegrasyonu ve kullanımı açıklanmaktadır.

Komponent şu npm paketlerine ihtiyaç duyar:

- axios (API istekleri için)
- date-fns (tarih formatlama için)
- date-fns/locale/tr (Türkçe tarih formatlaması için)

```bash
npm install axios date-fns
```

### Komponentin Çalışma Mantığı

1. **Veri Yükleme**: Komponent yüklendiğinde `loadChats()` metodu çağrılır ve ilk sayfa verisi yüklenir.
2. **Sonsuz Kaydırma (Infinite Scrolling)**: `IntersectionObserver` API'si kullanılarak liste sonuna gelindiğinde otomatik olarak daha fazla veri yüklenir.
3. **Kategorilere Ayırma**: Backend'den düz liste olarak gelen veriler frontend tarafında kategorilere ayrılır:
   - Bugün
   - Dün
   - Geçen Hafta (Son 7 gün içinde, bugün ve dün hariç)
   - Geçen Ay (Son 30 gün içinde, son 7 gün hariç)
   - Daha Eski (30 günden daha eski)

### Özelleştirme Seçenekleri

Komponentin bazı davranışlarını özelleştirmek için şu değişiklikleri yapabilirsiniz:

1. **Sayfa Boyutu Değiştirme**: `limit` değişkenini değiştirerek sayfa başına yüklenen öğe sayısını değiştirebilirsiniz.
2. **Sıralama Kriteri**: `/api/chat/flat-histories` endpoint'ine gönderilen `order` parametresini değiştirerek "created" veya "updated" bazlı sıralama yapabilirsiniz.
3. **CSS Özelleştirmesi**: `ChatListInfiniteScroll.css` dosyasındaki stilleri düzenleyerek görünümü özelleştirebilirsiniz.

### Sohbet Öğesine Tıklama Yönetimi

Mevcut durumda, bir sohbet öğesine tıklandığında herhangi bir eylem tanımlanmamıştır. Bunu uygulamanıza eklemek için şu şekilde yapabilirsiniz:

```tsx
// ChatListInfiniteScroll.tsx içinde:
const handleChatClick = (chatId: string) => {
  // Örneğin, sohbet sayfasına yönlendirme:
  window.location.href = `/chat/${chatId}`;
  // veya React Router kullanıyorsanız:
  // navigate(`/chat/${chatId}`);
};

// Render içinde:
<div
  key={chat.id}
  className="chat-item"
  onClick={() => handleChatClick(chat.id)}
  // ... diğer proplar
>
  {/* içerik */}
</div>;
```

## 4. Kullanım Senaryoları

### 1. Arama ve Filtreleme Ekleme

```tsx
const [searchTerm, setSearchTerm] = useState("");

const handleSearch = (e: React.FormEvent) => {
  e.preventDefault();
  // Aramayı uygula ve listeyi sıfırla
  setOffset(0);
  setChatItems([]);
  loadChats(); // searchTerm'i API çağrısına ekleyerek
};

// Formun render edilmesi:
<form onSubmit={handleSearch}>
  <input
    type="text"
    value={searchTerm}
    onChange={(e) => setSearchTerm(e.target.value)}
    placeholder="Sohbetlerde ara..."
  />
  <button type="submit">Ara</button>
</form>;
```

### 2. Kategoriye Göre Filtreleme

```tsx
const [activeCategory, setActiveCategory] = useState<string | null>(null);

const filteredChats = activeCategory
  ? categorizedChats[activeCategory as keyof typeof categorizedChats]
  : chatItems;

// Kategori seçiminin render edilmesi:
<div className="category-filter">
  <button
    onClick={() => setActiveCategory(null)}
    className={activeCategory === null ? "active" : ""}
  >
    Tümü
  </button>
  <button
    onClick={() => setActiveCategory("today")}
    className={activeCategory === "today" ? "active" : ""}
  >
    Bugün
  </button>
  {/* Diğer kategori butonları */}
</div>;
```

## 5. Performans Optimizasyonları

### Veri Önbelleğe Alma (Data Caching)

Uygulamanızda React Query veya SWR gibi araçları kullanarak API çağrılarını önbelleğe alabilirsiniz. Bu, tekrar tekrar aynı verileri yüklemekten kaçınmanızı sağlar.

```tsx
// React Query ile örnek kullanım:
import { useQuery, useInfiniteQuery } from "react-query";

const fetchChatHistory = ({ pageParam = 0 }) => {
  return axios
    .get("/api/chat/flat-histories", {
      params: {
        userId,
        offset: pageParam,
        limit,
        order: "updated",
      },
    })
    .then((res) => res.data);
};

const { data, fetchNextPage, hasNextPage, isFetching } = useInfiniteQuery(
  ["chatHistory", userId],
  fetchChatHistory,
  {
    getNextPageParam: (lastPage) => {
      const { offset, limit, total } = lastPage;
      return offset + limit < total ? offset + limit : undefined;
    },
  }
);
```

### Sanal Liste Kullanımı

Çok sayıda sohbet öğesi varsa, React Virtualized veya React Window gibi sanal liste kütüphaneleri kullanarak yalnızca görünür öğeleri render edebilirsiniz.

## 6. Hata Yönetimi

Komponente daha güçlü hata yönetimi ekleyebilirsiniz:

```tsx
const [error, setError] = useState<string | null>(null);

const loadChats = async () => {
  try {
    setIsLoading(true);
    setError(null);
    // API çağrısı...
  } catch (error) {
    console.error("Sohbet listesi yüklenirken hata:", error);
    setError(
      "Sohbet listesi yüklenirken bir hata oluştu. Lütfen tekrar deneyin."
    );
  } finally {
    setIsLoading(false);
  }
};

// Render içinde:
{
  error && <div className="error-message">{error}</div>;
}
```

## 7. API Endpoint'inin Uyarlanması

API endpoint'i şu durumlar için özelleştirilebilir:

- **Arşivlenmiş sohbetleri gösterme/gizleme**: `showArchived` parametresi eklenebilir
- **Arama özelliği**: `searchQuery` parametresi eklenebilir
- **Belirli konulara göre filtreleme**: `tags` veya `topics` parametresi eklenebilir

## 8. Frontend Ekibine Notlar

1. `ChatListInfiniteScroll.tsx` ve `ChatListInfiniteScroll.css` dosyalarını `components/ChatList/` dizinine ekleyiniz.
2. API endpoint'i `/ai/chat/flat-histories` olarak ayarlandı.
3. Kullanıcı kimlik doğrulaması ve token yönetimi kodda gösterilmemiştir, mevcut kimlik doğrulama sisteminizle entegre etmeniz gerekecektir.
4. Yükleme göstergesi, hata durumları ve boş durum mesajları bileşene dahil edilmiştir, ancak tasarımınıza göre bu öğeleri özelleştirebilirsiniz.
5. Endpoint'lerin tamamı test edilmiş ve ChatGPT benzeri bir sohbet listesi deneyimi sağlamak için gerekli tüm özellikleri içermektedir.
