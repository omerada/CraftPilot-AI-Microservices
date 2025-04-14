# AI Performance Fixer API Entegrasyon Dokümanı

Bu doküman, frontend geliştiricilerine AI Performance Fixer modülüne nasıl entegre olunacağı konusunda rehberlik etmek üzere hazırlanmıştır.

## Genel Bakış

AI Performance Fixer, web sitelerinin performansını analiz eden ve iyileştirme önerileri sunan bir API koleksiyonudur. Bu API'ler aşağıdaki temel işlemleri gerçekleştirir:

1. **Performans Analizi**: Web sitesinin performansını Lighthouse kullanarak analiz eder
2. **Iyileştirme Önerileri**: Tespit edilen sorunlar için AI destekli öneriler sunar
3. **Performans Geçmişi**: Belirli bir URL için geçmiş performans analizlerini gösterir

## Endpoint'ler

Tüm isteklerin `/api/performance` yolu üzerinden yapıldığını unutmayın.

### 1. Performans Analiz Endpoint'i

**Endpoint**: `POST /api/performance/analyze`

**İstek Örneği:**

```javascript
const response = await fetch("/api/performance/analyze", {
  method: "POST",
  headers: {
    "Content-Type": "application/json",
    Authorization: "Bearer <JWT_TOKEN>",
  },
  body: JSON.stringify({
    url: "https://example.com",
  }),
});

const result = await response.json();
```

**Yanıt Örneği:**

```json
{
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
    "total-blocking-time": {
      "score": 0.75,
      "displayValue": "180ms",
      "description": "Sum of all time periods between FCP and Time to Interactive"
    },
    "cumulative-layout-shift": {
      "score": 0.92,
      "displayValue": "0.05",
      "description": "Cumulative Layout Shift measures visual stability"
    },
    "speed-index": {
      "score": 0.88,
      "displayValue": "2.8s",
      "description": "Speed Index shows how quickly the contents of a page are visibly populated"
    }
  },
  "timestamp": 1673912345678,
  "url": "https://example.com",
  "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "categories": {
    "performance": { "score": 0.85 },
    "accessibility": { "score": 0.92 },
    "best-practices": { "score": 0.87 },
    "seo": { "score": 0.95 }
  }
}
```

### 2. AI Önerileri Endpoint'i

**Endpoint**: `POST /api/performance/suggestions`

**İstek Örneği:**

```javascript
const response = await fetch("/api/performance/suggestions", {
  method: "POST",
  headers: {
    "Content-Type": "application/json",
    Authorization: "Bearer <JWT_TOKEN>",
  },
  body: JSON.stringify({
    analysisData: performanceAnalysisResult, // 1. endpoint'den alınan yanıt
  }),
});

const suggestions = await response.json();
```

**Yanıt Örneği:**

```json
{
  "content": "[{\"problem\":\"Optimize edilmemiş görseller\",\"severity\":\"critical\",\"solution\":\"Görselleri WebP formatına dönüştürün ve boyutlarını düşürün\",\"codeExample\":\"<!-- Örnek kod -->\\n<picture>\\n  <source srcset=\\\"image.webp\\\" type=\\\"image/webp\\\">\\n  <img src=\\\"image.jpg\\\" loading=\\\"lazy\\\" alt=\\\"Açıklama\\\">\\n</picture>\",\"resources\":[\"https://web.dev/optimize-images\"],\"implementationDifficulty\":\"easy\"},{\"problem\":\"Render-blocking kaynaklar\",\"severity\":\"major\",\"solution\":\"Kritik CSS'i inline olarak ekleyin ve JS yüklemelerini erteleyebilirsiniz\",\"codeExample\":\"<!-- Kritik CSS -->\\n<style>\\n  /* Kritik stiller burada */\\n</style>\\n\\n<!-- JS erteleme -->\\n<script src=\\\"script.js\\\" defer></script>\",\"resources\":[\"https://web.dev/render-blocking-resources\"],\"implementationDifficulty\":\"medium\"}]"
}
```

> **Not:** `content` değeri bir JSON string'idir. Kullanmadan önce parse edilmesi gerekir:
>
> ```javascript
> const suggestions = JSON.parse(response.content);
> ```

### 3. Performans Geçmişi Endpoint'i

**Endpoint**: `POST /api/performance/history`

**İstek Örneği:**

```javascript
const response = await fetch("/api/performance/history", {
  method: "POST",
  headers: {
    "Content-Type": "application/json",
    Authorization: "Bearer <JWT_TOKEN>",
  },
  body: JSON.stringify({
    url: "https://example.com",
  }),
});

const history = await response.json();
```

**Yanıt Örneği:**

```json
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

## Hata İşleme

API'ler aşağıdaki HTTP durum kodlarını döndürebilir:

| Durum Kodu | Açıklama                               |
| ---------- | -------------------------------------- |
| 200        | Başarılı                               |
| 201        | Kaynak oluşturuldu                     |
| 400        | Geçersiz istek (URL doğrulanamadı vb.) |
| 429        | Rate limit aşıldı (dakikada 5 istek)   |
| 500        | Sunucu hatası                          |

## Örnek Frontend Kullanımı

### React ile Örnek Kullanım

```jsx
import React, { useState } from "react";
import axios from "axios";

function PerformanceFixer() {
  const [url, setUrl] = useState("");
  const [loading, setLoading] = useState(false);
  const [analysis, setAnalysis] = useState(null);
  const [suggestions, setSuggestions] = useState([]);
  const [history, setHistory] = useState([]);
  const [error, setError] = useState(null);

  const analyzeWebsite = async () => {
    try {
      setLoading(true);
      setError(null);

      // Adım 1: Performans analizi yap
      const analysisResponse = await axios.post("/api/performance/analyze", {
        url,
      });
      const analysisData = analysisResponse.data;
      setAnalysis(analysisData);

      // Adım 2: AI önerilerini al
      const suggestionsResponse = await axios.post(
        "/api/performance/suggestions",
        {
          analysisData,
        }
      );

      // JSON stringini parse et
      const parsedSuggestions = JSON.parse(suggestionsResponse.data.content);
      setSuggestions(parsedSuggestions);

      // Adım 3: Geçmiş analiz verilerini getir
      const historyResponse = await axios.post("/api/performance/history", {
        url,
      });
      setHistory(historyResponse.data.history);
    } catch (err) {
      console.error("Error analyzing website", err);
      if (err.response && err.response.status === 429) {
        setError("Rate limit aşıldı. Lütfen biraz bekleyip tekrar deneyin.");
      } else {
        setError("Web sitesi analiz edilirken bir hata oluştu.");
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="performance-fixer">
      <h1>Web Sitesi Performans Analizi</h1>

      <div className="input-container">
        <input
          type="text"
          value={url}
          onChange={(e) => setUrl(e.target.value)}
          placeholder="https://example.com"
        />
        <button onClick={analyzeWebsite} disabled={loading || !url}>
          {loading ? "Analiz Ediliyor..." : "Analiz Et"}
        </button>
      </div>

      {error && <div className="error">{error}</div>}

      {analysis && (
        <div className="analysis-results">
          <h2>Performans Analizi</h2>
          <div className="score-card">
            <div className="score">
              {Math.round(analysis.performance * 100)}
            </div>
            <div className="label">Performans Skoru</div>
          </div>

          <div className="metrics">
            {Object.entries(analysis.audits).map(([key, audit]) => (
              <div className="metric" key={key}>
                <div className="metric-name">{key}</div>
                <div className="metric-value">{audit.displayValue}</div>
                <div
                  className="metric-score"
                  style={{
                    backgroundColor:
                      audit.score > 0.89
                        ? "green"
                        : audit.score > 0.49
                        ? "orange"
                        : "red",
                  }}
                >
                  {Math.round(audit.score * 100)}
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {suggestions.length > 0 && (
        <div className="suggestions">
          <h2>İyileştirme Önerileri</h2>
          {suggestions.map((suggestion, index) => (
            <div className="suggestion-card" key={index}>
              <h3>{suggestion.problem}</h3>
              <div className="severity" data-severity={suggestion.severity}>
                {suggestion.severity}
              </div>
              <p>{suggestion.solution}</p>

              <div className="code-example">
                <h4>Kod Örneği:</h4>
                <pre>
                  <code>{suggestion.codeExample}</code>
                </pre>
              </div>

              <div className="resources">
                <h4>Kaynaklar:</h4>
                <ul>
                  {suggestion.resources.map((resource, i) => (
                    <li key={i}>
                      <a
                        href={resource}
                        target="_blank"
                        rel="noopener noreferrer"
                      >
                        {resource}
                      </a>
                    </li>
                  ))}
                </ul>
              </div>

              <div className="difficulty">
                Zorluk: {suggestion.implementationDifficulty}
              </div>
            </div>
          ))}
        </div>
      )}

      {history.length > 0 && (
        <div className="history">
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

export default PerformanceFixer;
```

### Vue.js ile Örnek Kullanım

```vue
<template>
  <div class="performance-fixer">
    <h1>Web Sitesi Performans Analizi</h1>

    <div class="input-container">
      <input type="text" v-model="url" placeholder="https://example.com" />
      <button @click="analyzeWebsite" :disabled="loading || !url">
        {{ loading ? "Analiz Ediliyor..." : "Analiz Et" }}
      </button>
    </div>

    <div v-if="error" class="error">{{ error }}</div>

    <div v-if="analysis" class="analysis-results">
      <!-- Analiz sonuçları gösterimi -->
      <!-- ... -->
    </div>

    <div v-if="suggestions.length" class="suggestions">
      <!-- İyileştirme önerileri gösterimi -->
      <!-- ... -->
    </div>

    <div v-if="history.length" class="history">
      <!-- Performans geçmişi gösterimi -->
      <!-- ... -->
    </div>
  </div>
</template>

<script>
import axios from "axios";

export default {
  name: "PerformanceFixer",
  data() {
    return {
      url: "",
      loading: false,
      analysis: null,
      suggestions: [],
      history: [],
      error: null,
    };
  },
  methods: {
    async analyzeWebsite() {
      try {
        this.loading = true;
        this.error = null;

        // Adım 1: Performans analizi yap
        const analysisResponse = await axios.post("/api/performance/analyze", {
          url: this.url,
        });
        this.analysis = analysisResponse.data;

        // Adım 2: AI önerilerini al
        const suggestionsResponse = await axios.post(
          "/api/performance/suggestions",
          {
            analysisData: this.analysis,
          }
        );

        // JSON stringini parse et
        this.suggestions = JSON.parse(suggestionsResponse.data.content);

        // Adım 3: Geçmiş analiz verilerini getir
        const historyResponse = await axios.post("/api/performance/history", {
          url: this.url,
        });
        this.history = historyResponse.data.history;
      } catch (err) {
        console.error("Error analyzing website", err);
        if (err.response && err.response.status === 429) {
          this.error =
            "Rate limit aşıldı. Lütfen biraz bekleyip tekrar deneyin.";
        } else {
          this.error = "Web sitesi analiz edilirken bir hata oluştu.";
        }
      } finally {
        this.loading = false;
      }
    },
  },
};
</script>
```

## Önemli Notlar

1. **Rate Limiting**: API, IP başına dakikada 5 istek ile sınırlıdır. Bu limiti aşarsanız `429 Too Many Requests` hatası alırsınız.

2. **Analiz Süresi**: Performans analizi uzun sürebilir (30-60 saniye). Frontend'de kullanıcıya uygun bir yükleme göstergesi göstermeyi unutmayın.

3. **Güvenlik**: Tüm istekler için geçerli bir JWT token'ı gereklidir.

4. **Önbellekleme**: Aynı URL için kısa süre içinde tekrar analiz istemekten kaçının. Backend tarafında önbelleğe alınmış sonuçlar döndürülecektir.

5. **Content Parsing**: `suggestions` endpoint'inden dönen `content` değeri bir JSON string'idir. Kullanmadan önce `JSON.parse()` ile ayrıştırılması gerekir.

6. **Metrik Renklendirme**: Frontend'de metrikleri görselleştirirken aşağıdaki renk kodlaması önerilir:

   - **Yeşil (İyi)**: Skor >= 0.9 (90%)
   - **Turuncu (Orta)**: Skor >= 0.5 (50%)
   - **Kırmızı (Kötü)**: Skor < 0.5 (50%)

## Sorunlarla Karşılaşırsanız

Entegrasyon sırasında herhangi bir sorunla karşılaşırsanız, lütfen backend ekibiyle iletişime geçin:

- **Slack Kanalı**: #performance-api-integration
- **E-posta**: backend-team@craftpilot.ai
- **Jira Board**: PERF-API projesinde yeni bir ticket açın

---

Bu dokümanın en son güncellenme tarihi: [GÜNCEL TARİH]
