# AI Performance Fixer Backend Gereksinimleri

## Temel Endpointler

### 1. Performans Analiz Endpoint'i

POST /api/performance/analyze

**İstek:**

```json
{
    "url": "https://example.com"
}
```

**Yanıt:**

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
        }
        // Diğer önemli metrikleri içerir
    },
    "timestamp": 1673912345678,
    "url": "https://example.com",
    "categories": {
        "performance": { "score": 0.85 },
        "accessibility": { "score": 0.92 },
        "best-practices": { "score": 0.87 },
        "seo": { "score": 0.95 }
    }
}
```

### 2. AI Önerileri Endpoint'i

POST /api/performance/suggestions

**İstek:**

```json
{
    "url": "https://example.com"
}
```

**Yanıt:**

```json
{
    "suggestions": [
        {
            "id": "suggestion-1",
            "problem": "Optimize edilmemiş görseller",
            "severity": "critical",
            "solution": "Görselleri WebP formatına dönüştürün ve boyutlarını düşürün",
            "codeExample": "<!-- Örnek kod -->\n<picture>\n  <source srcset=\"image.webp\" type=\"image/webp\">\n  <img src=\"image.jpg\" loading=\"lazy\" alt=\"Açıklama\">\n</picture>",
            "resources": ["https://web.dev/optimize-images"],
            "implementationDifficulty": "easy"
        },
        {
            "id": "suggestion-2",
            "problem": "Render-blocking kaynaklar",
            "severity": "major",
            "solution": "Kritik CSS'i inline olarak ekleyin ve JS yüklemelerini erteleyebilirsiniz",
            "codeExample": "<!-- Kritik CSS -->\n<style>\n  /* Kritik stiller burada */\n</style>\n\n<!-- JS erteleme -->\n<script src=\"script.js\" defer></script>",
            "resources": ["https://web.dev/render-blocking-resources"],
            "implementationDifficulty": "medium"
        }
    ]
}
```

### 3. Performans Geçmişi Endpoint'i (Opsiyonel)

POST /api/performance/history

**İstek:**

```json
{
    "url": "https://example.com"
}
```

**Yanıt:**

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

## Teknik Gereksinimler

-   Lighthouse Entegrasyonu: Headless Chrome çalıştırarak Lighthouse analizleri yapılmalı
-   Maksimum Yanıt Süresi: Analiz endpoint'i maksimum 60 saniye içinde yanıt vermeli
-   AI Modeli: Gemini API entegrasyonu ile performans önerileri üretilmeli
-   Hata İşleme: URL'in erişilebilir olmama durumu için uygun hata mesajları döndürmeli

## Güvenlik

-   URL doğrulama ve sanitizasyon
-   Rate limiting (IP başına dakikada 5 istek)

## Notlar

-   Tüm endpoint'ler JSON formatında yanıt döndürmeli
-   Backend'de performans analizi yapılmalı, frontend'de değil
-   Önbellek mekanizması ile aynı URL için kısa süre içinde yapılan tekrarlı istekler optimize edilmeli
