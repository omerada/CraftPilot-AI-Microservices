const { logger } = require("./logger");
const path = require("path");
const fs = require("fs");
const os = require("os");

// Lighthouse analizi çalıştırma
async function runLighthouseAnalysis(url, analysisType = "basic") {
  logger.info(
    `Starting Lighthouse analysis for ${url} with type: ${analysisType}`
  );

  let chrome = null;
  let results = null;
  let tempOutputPath = null;

  try {
    // Geçici dosya oluştur
    const tempDir = process.env.LIGHTHOUSE_TEMP_DIR || os.tmpdir();
    tempOutputPath = path.join(tempDir, `lighthouse-${Date.now()}.json`);

    // ESM modüllerini dinamik olarak import et
    const chromeLauncher = await import("chrome-launcher");
    const lighthouse = await import("lighthouse");

    // Chrome'u başlat, görsel işlemlerini devre dışı bırak
    chrome = await chromeLauncher.launch({
      chromeFlags: [
        "--headless",
        "--no-sandbox",
        "--disable-gpu",
        "--disable-dev-shm-usage",
        "--disable-software-rasterizer",
        "--disable-images", // Görsel yüklemeyi devre dışı bırak
        "--blink-settings=imagesEnabled=false", // Blink motorunda görüntüleri devre dışı bırak
      ],
    });

    // Lighthouse ayarları ve kategorilerini belirle
    const categories =
      analysisType === "detailed"
        ? ["performance", "accessibility", "best-practices", "seo"]
        : ["performance"];

    // Her iki analiz tipi için optimize edilmiş ayarlar
    const opts = {
      logLevel: "info",
      output: "json",
      onlyCategories: categories,
      port: chrome.port,
      locale: "tr", // Türkçe dil desteği
      // Analiz hızını artıracak ayarlar
      disableStorageReset: true,
      formFactor: "desktop",
      throttlingMethod: "simulate",
      screenEmulation: {
        disabled: true,
      },
      // Ekran görüntüsü ve görsel denetimlerini devre dışı bırak
      skipAudits: [
        // Görsel denetimleri atla
        "screenshot-thumbnails",
        "final-screenshot",
        "full-page-screenshot",
        "uses-optimized-images",
        "uses-webp-images",
        "uses-responsive-images",
        "offscreen-images",

        // Analiz tipine göre fazladan denetimleri atla
        ...(analysisType === "basic"
          ? [
              // Basic analiz için ek olarak atlanan denetimler
              "largest-contentful-paint",
              "cumulative-layout-shift",
              "third-party-facades",
              "third-party-summary",
              "unsized-images",
            ]
          : []),
      ],
    };

    // Lighthouse'u çalıştır (default export'u kullanarak)
    results = await lighthouse.default(url, opts);

    // JSON çıktısını yazdır
    fs.writeFileSync(tempOutputPath, results.report);

    // JSON içeriğini parse et
    const jsonResults = JSON.parse(results.report);

    // Geçici dosyayı temizle
    fs.unlinkSync(tempOutputPath);

    // Chrome'u kapat
    await chrome.kill();

    return jsonResults;
  } catch (error) {
    logger.error(`Lighthouse analysis failed: ${error.message}`);

    // Hata durumunda kaynakları temizle
    if (chrome) {
      await chrome.kill();
    }

    if (tempOutputPath && fs.existsSync(tempOutputPath)) {
      try {
        fs.unlinkSync(tempOutputPath);
      } catch (e) {
        logger.warn(
          `Failed to remove temp file ${tempOutputPath}: ${e.message}`
        );
      }
    }

    throw error;
  }
}

module.exports = { runLighthouseAnalysis };
