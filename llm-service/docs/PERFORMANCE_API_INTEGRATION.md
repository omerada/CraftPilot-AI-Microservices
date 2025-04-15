# Web Performans Analizi API Entegrasyon Rehberi

Bu rehber, Web Performans Analizi API'lerini frontend uygulamanıza nasıl entegre edeceğinizi anlatmaktadır.

## Genel Bakış

Performans Analizi API'leri, web sitelerinin performansını analiz etmek, iyileştirme önerileri almak ve geçmiş performans analizlerini görüntülemek için kullanılır.

### Temel Özellikler

1. **Performans Analizi:** Web sitesinin performansını Lighthouse ile analiz eder.
2. **AI İyileştirme Önerileri:** Tespit edilen performans sorunları için yapay zeka destekli çözüm önerileri sunar.
3. **Performans Geçmişi:** Belirli bir URL için geçmiş performans analizlerini görüntüler.

## API Endpoint'leri

Tüm API çağrıları `/api/performance` yolu üzerinden yapılır.

### 1. Performans Analizi

**Endpoint:** `POST /api/performance/analyze`

**Açıklama:** Belirtilen URL'nin performans analizini başlatır. Bu işlem asenkron olarak çalışır ve hemen sonuç dönmeyebilir.

**İstek:**

```javascript
{
  "url": "https://example.com"
}
```

**Yanıt Durumları:**

1. **İşlem Hemen Tamamlandı:**

   ```javascript
   {
     "id": "abc123",
     "performance": 0.85,
     "audits": {
       "first-contentful-paint": {
         "score": 0.9,
         "displayValue": "1.2s",
         "description": "First Contentful Paint marks the time at which the first text or image is painted"
       },
       "largest-contentful-paint": {
         "score": 0.8,
         "displayValue": "2.5s",
         "description": "Largest Contentful Paint marks the time at which the largest text or image is painted"
       },
       // ...diğer audit detayları
     },
     "url": "https://example.com",
     "timestamp": 1673912345678,
     "categories": {
       "performance": { "score": 0.85 },
       "accessibility": { "score": 0.92 },
       "best-practices": { "score": 0.87 },
       "seo": { "score": 0.95 }
     }
   }
   ```

2. **İşlem Devam Ediyor (Asenkron):**
   ```javascript
   {
     "jobId": "job-abc123",
     "url": "https://example.com",
     "status": "PENDING",
     "message": "Analiz devam ediyor, lütfen daha sonra tekrar deneyin"
   }
   ```

### 2. Analiz Durumu Sorgulama

**Endpoint:** `GET /api/performance/status/{jobId}`

**Açıklama:** Devam eden bir analizin durumunu sorgulamak için kullanılır.

**Yanıt (İşlem Devam Ediyor):**

```javascript
{
  "jobId": "job-abc123",
  "status": "PENDING",
  "complete": false,
  "message": "Analiz devam ediyor, lütfen daha sonra tekrar deneyin"
}
```

**Yanıt (İşlem Tamamlandı):**

```javascript
{
  "jobId": "job-abc123",
  "status": "COMPLETED",
  "complete": true,
  // ...analiz sonuçları (1. endpoint'teki gibi)
}
```

### 3. AI Önerileri

**Endpoint:** `POST /api/performance/suggestions`

**Açıklama:** Performans analizi sonuçlarına göre AI destekli iyileştirme önerileri üretir.

**İstek:**

```javascript
{
  "analysisData": {
    // Performans analizi sonuçları (1. endpoint'ten alınan yanıt)
  }
}
```

**Yanıt:**

```javascript
{
  "content": "[{\"problem\":\"Optimize edilmemiş görseller\",\"severity\":\"critical\",\"solution\":\"Görselleri WebP formatına dönüştürün ve boyutlarını düşürün\",\"codeExample\":\"<!-- Örnek kod -->\\n<picture>\\n  <source srcset=\\\"image.webp\\\" type=\\\"image/webp\\\">\\n  <img src=\\\"image.jpg\\\" loading=\\\"lazy\\\" alt=\\\"Açıklama\\\">\\n</picture>\",\"resources\":[\"https://web.dev/optimize-images\"],\"implementationDifficulty\":\"easy\"}, ...]"
}
```

> **Önemli Not:** `content` alanı JSON string formatındadır. Kullanmadan önce `JSON.parse()` ile işlenmelidir.

### 4. Performans Geçmişi

**Endpoint:** `POST /api/performance/history`

**Açıklama:** Belirtilen URL için geçmiş performans analiz sonuçlarını getirir.

**İstek:**

```javascript
{
  "url": "https://example.com"
}
```

**Yanıt:**

```javascript
{
  "history": [
    {
      "id": "analysis-123",
      "url": "https://example.com",
      "timestamp": 1673912345678,
      "performance": 0.85
    },
    {
      "id": "analysis-122",
      "url": "https://example.com",
      "timestamp": 1673825945678,
      "performance": 0.82
    }
  ]
}
```

## Kullanım Senaryoları

### 1. Temel Kullanım Senaryosu

Frontend'de en sık kullanılan senaryo aşağıdaki adımları içerir:

1. Kullanıcı URL girer ve analiz başlatır
2. Kullanıcıya "analiz devam ediyor" mesajı gösterilir
3. Backend'den sonuç alınıncaya kadar düzenli aralıklarla sorgu yapılır
4. Analiz tamamlandığında sonuçlar gösterilir
5. İstenirse AI önerileri istenir ve gösterilir
6. İstenirse performans geçmişi görüntülenir

### 2. Asenkron İşlem Yönetimi

Performans analizi uzun sürebilir (30-60 saniye). Bu nedenle, işlem durumunu yönetmek için polling kullanılmalıdır:

```javascript
async function analyzeWebsite(url) {
  try {
    // 1. Analiz başlat
    const response = await api.post("/api/performance/analyze", { url });

    // 2. Durumu kontrol et
    if (response.data.status === "PENDING") {
      // Analiz devam ediyor, jobId'yi sakla
      const jobId = response.data.jobId;
      return startPollingForResults(jobId);
    } else {
      // Analiz hemen tamamlandı
      return response.data;
    }
  } catch (error) {
    console.error("Analiz hatası:", error);
    throw error;
  }
}

async function startPollingForResults(jobId) {
  return new Promise((resolve, reject) => {
    const maxAttempts = 30; // Maksimum 30 deneme (90 saniye)
    let attempts = 0;

    const checkStatus = async () => {
      try {
        const statusResponse = await api.get(
          `/api/performance/status/${jobId}`
        );
        const result = statusResponse.data;

        if (result.complete === true) {
          // İşlem tamamlandı
          clearInterval(interval);
          resolve(result);
        } else if (++attempts >= maxAttempts) {
          // Zaman aşımı
          clearInterval(interval);
          reject(new Error("Analiz zaman aşımına uğradı"));
        }
      } catch (error) {
        clearInterval(interval);
        reject(error);
      }
    };

    // 3 saniyede bir kontrol et
    const interval = setInterval(checkStatus, 3000);
  });
}
```

## Örnek İmplementasyonu (React)

```jsx
import React, { useState, useEffect } from "react";
import axios from "axios";

const api = axios.create({
  baseURL: "/api",
  headers: {
    "Content-Type": "application/json",
  },
});

function PerformanceAnalyzer() {
  const [url, setUrl] = useState("");
  const [loading, setLoading] = useState(false);
  const [status, setStatus] = useState("idle"); // idle, pending, completed, error
  const [jobId, setJobId] = useState(null);
  const [analysisData, setAnalysisData] = useState(null);
  const [suggestions, setSuggestions] = useState([]);
  const [history, setHistory] = useState([]);
  const [error, setError] = useState(null);
  const [pollingInterval, setPollingInterval] = useState(null);

  // Polling fonksiyonu
  useEffect(() => {
    if (status === "pending" && jobId) {
      const interval = setInterval(async () => {
        try {
          const response = await api.get(`/performance/status/${jobId}`);
          if (response.data.complete) {
            clearInterval(interval);
            setStatus("completed");
            setAnalysisData(response.data);
            setLoading(false);
          }
        } catch (err) {
          clearInterval(interval);
          setError("Analiz durumu kontrol edilirken hata oluştu");
          setStatus("error");
          setLoading(false);
        }
      }, 3000);

      setPollingInterval(interval);

      return () => clearInterval(interval);
    }
  }, [status, jobId]);

  const startAnalysis = async () => {
    if (!url) return;

    setLoading(true);
    setError(null);
    setStatus("idle");
    setAnalysisData(null);
    setSuggestions([]);

    try {
      const response = await api.post("/performance/analyze", { url });

      if (response.data.status === "PENDING") {
        // Asenkron işlem başlatıldı
        setStatus("pending");
        setJobId(response.data.jobId);
      } else {
        // İşlem hemen tamamlandı
        setStatus("completed");
        setAnalysisData(response.data);
        setLoading(false);
      }
    } catch (err) {
      setError("Analiz başlatılırken bir hata oluştu");
      setStatus("error");
      setLoading(false);
    }
  };

  const getSuggestions = async () => {
    if (!analysisData) return;

    try {
      const response = await api.post("/performance/suggestions", {
        analysisData,
      });

      // JSON stringi parse et
      const parsedSuggestions = JSON.parse(response.data.content);
      setSuggestions(parsedSuggestions);
    } catch (err) {
      setError("Öneriler alınırken bir hata oluştu");
    }
  };

  const getHistory = async () => {
    if (!url) return;

    try {
      const response = await api.post("/performance/history", { url });
      setHistory(response.data.history || []);
    } catch (err) {
      console.error("Geçmiş alınırken bir hata oluştu", err);
    }
  };

  return (
    <div className="performance-analyzer">
      <h1>Web Sitesi Performans Analizi</h1>

      {/* URL Giriş Formu */}
      <div className="input-section">
        <input
          type="text"
          value={url}
          onChange={(e) => setUrl(e.target.value)}
          placeholder="https://example.com"
          disabled={loading}
        />
        <button onClick={startAnalysis} disabled={loading || !url}>
          {loading ? "Analiz Ediliyor..." : "Analiz Et"}
        </button>
      </div>

      {/* Hata Mesajı */}
      {error && <div className="error-message">{error}</div>}

      {/* Analiz Devam Ediyor Göstergesi */}
      {status === "pending" && (
        <div className="status-message">
          <div className="spinner"></div>
          <p>Analiz devam ediyor, lütfen bekleyin...</p>
        </div>
      )}

      {/* Analiz Sonuçları */}
      {status === "completed" && analysisData && (
        <div className="results-section">
          <h2>Performans Analizi Sonuçları</h2>

          {/* Performans Skoru */}
          <div className="performance-score">
            <div
              className="score"
              style={{
                backgroundColor:
                  analysisData.performance > 0.89
                    ? "#0cce6b"
                    : analysisData.performance > 0.49
                    ? "#ffa400"
                    : "#ff4e42",
              }}
            >
              {Math.round(analysisData.performance * 100)}
            </div>
            <span>Performans Skoru</span>
          </div>

          {/* Detaylı Metrikleri Göster */}
          <div className="metrics-grid">
            {analysisData.audits &&
              Object.entries(analysisData.audits).map(([key, audit]) => (
                <div className="metric-card" key={key}>
                  <h3>{audit.description || key}</h3>
                  <div className="metric-details">
                    <div className="metric-value">{audit.displayValue}</div>
                    <div
                      className="metric-score"
                      style={{
                        backgroundColor:
                          audit.score > 0.89
                            ? "#0cce6b"
                            : audit.score > 0.49
                            ? "#ffa400"
                            : "#ff4e42",
                      }}
                    >
                      {Math.round(audit.score * 100)}
                    </div>
                  </div>
                </div>
              ))}
          </div>

          {/* İyileştirme Önerileri Butonu */}
          {!suggestions.length && (
            <button onClick={getSuggestions} className="suggestions-button">
              İyileştirme Önerileri Al
            </button>
          )}

          {/* Geçmiş Butonu */}
          <button onClick={getHistory} className="history-button">
            Performans Geçmişini Göster
          </button>
        </div>
      )}

      {/* İyileştirme Önerileri */}
      {suggestions.length > 0 && (
        <div className="suggestions-section">
          <h2>İyileştirme Önerileri</h2>

          {suggestions.map((suggestion, index) => (
            <div
              key={index}
              className="suggestion-card"
              data-severity={suggestion.severity}
            >
              <h3>{suggestion.problem}</h3>
              <div className="severity-badge">{suggestion.severity}</div>
              <p>{suggestion.solution}</p>

              {suggestion.codeExample && (
                <div className="code-example">
                  <h4>Örnek Kod:</h4>
                  <pre>{suggestion.codeExample}</pre>
                </div>
              )}

              {suggestion.resources && (
                <div className="resources">
                  <h4>Kaynaklar:</h4>
                  <ul>
                    {suggestion.resources.map((link, i) => (
                      <li key={i}>
                        <a
                          href={link}
                          target="_blank"
                          rel="noopener noreferrer"
                        >
                          {link}
                        </a>
                      </li>
                    ))}
                  </ul>
                </div>
              )}

              <div className="difficulty">
                Zorluk: {suggestion.implementationDifficulty}
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Performans Geçmişi */}
      {history.length > 0 && (
        <div className="history-section">
          <h2>Performans Geçmişi</h2>

          <table>
            <thead>
              <tr>
                <th>Tarih</th>
                <th>Performans Skoru</th>
              </tr>
            </thead>
            <tbody>
              {history.map((item) => (
                <tr key={item.id}>
                  <td>{new Date(item.timestamp).toLocaleString()}</td>
                  <td>{Math.round(item.performance * 100)}%</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}

export default PerformanceAnalyzer;
```

## Örnek İmplementasyonu (Vue.js)

```vue
<template>
  <div class="performance-analyzer">
    <h1>Web Sitesi Performans Analizi</h1>

    <!-- URL Giriş Formu -->
    <div class="input-section">
      <input
        type="text"
        v-model="url"
        placeholder="https://example.com"
        :disabled="loading"
      />
      <button @click="startAnalysis" :disabled="loading || !url">
        {{ loading ? "Analiz Ediliyor..." : "Analiz Et" }}
      </button>
    </div>

    <!-- Hata Mesajı -->
    <div v-if="error" class="error-message">{{ error }}</div>

    <!-- Analiz Devam Ediyor Göstergesi -->
    <div v-if="status === 'pending'" class="status-message">
      <div class="spinner"></div>
      <p>Analiz devam ediyor, lütfen bekleyin...</p>
    </div>

    <!-- Analiz Sonuçları -->
    <div v-if="status === 'completed' && analysisData" class="results-section">
      <h2>Performans Analizi Sonuçları</h2>

      <!-- Devam eden implementasyon... -->
    </div>

    <!-- İyileştirme Önerileri -->
    <div v-if="suggestions.length > 0" class="suggestions-section">
      <!-- Öneri kartları... -->
    </div>

    <!-- Performans Geçmişi -->
    <div v-if="history.length > 0" class="history-section">
      <!-- Geçmiş tablosu... -->
    </div>
  </div>
</template>

<script>
import axios from "axios";

export default {
  name: "PerformanceAnalyzer",
  data() {
    return {
      url: "",
      loading: false,
      status: "idle", // idle, pending, completed, error
      jobId: null,
      analysisData: null,
      suggestions: [],
      history: [],
      error: null,
      pollingInterval: null,
    };
  },
  methods: {
    async startAnalysis() {
      // Analiz başlatma implementasyonu...
    },
    async getSuggestions() {
      // Önerileri alma implementasyonu...
    },
    async getHistory() {
      // Geçmişi alma implementasyonu...
    },
  },
  watch: {
    // jobId veya status değiştiğinde polling işlemini yönetme
  },
  beforeUnmount() {
    // Component temizleme işlemleri
    if (this.pollingInterval) {
      clearInterval(this.pollingInterval);
    }
  },
};
</script>
```

## Önemli Notlar

1. **Asenkron İşlem Yönetimi:** Performans analizi uzun sürebilir, bu nedenle polling yaklaşımı kullanılmalıdır.

2. **Rate Limiting:** API'ler, IP başına dakikada 5 istek ile sınırlıdır. Bu limiti aşmanız durumunda 429 hatası alırsınız.

3. **Öneri İçeriği Ayrıştırma:** Suggestions endpoint'inden dönen `content` alanı bir JSON string'idir. Kullanmadan önce JSON.parse ile ayrıştırılmalıdır.

4. **Hata İşleme:** Tüm API çağrılarında uygun hata işleme mekanizmaları kullanılmalıdır.

5. **UI Durumları:** Frontend'de en az 4 durum yönetilmelidir: boş, yükleniyor, hata ve tamamlandı.

## Sık Karşılaşılan Sorunlar ve Çözümleri

1. **Hata:** 429 Too Many Requests
   **Çözüm:** Rate limiting nedeniyle oluşur. İstekler arasında zaman bırakarak veya retry mekanizması ekleyerek çözebilirsiniz.

2. **Hata:** URL cannot be analyzed
   **Çözüm:** URL erişilebilir değil veya format yanlış. URL'in http/https ile başladığından emin olun.

3. **Hata:** İşlem uzun süre "PENDING" durumunda kalıyor
   **Çözüm:** Bazı siteler analiz için çok büyük veya karmaşık olabilir. En fazla 90 saniye bekleyin ve sonra timeout olarak işleyin.

4. **Hata:** Suggestions içeriği parse edilemiyor
   **Çözüm:** API'den dönen içerik beklenmedik bir formatta olabilir. Try/catch bloğu ile JSON.parse işlemini yönetin.

## Yardım ve Destek

API entegrasyonunda sorun yaşarsanız, lütfen aşağıdaki kanallardan destek alın:

- **Slack:** #performance-api-integration
- **Mail:** frontend-support@craftpilot.ai
- **Jira:** PERF-API projesi altında ticket açın

---

Bu dokümantasyon, 26.11.2023 tarihinde güncellenmiştir.
