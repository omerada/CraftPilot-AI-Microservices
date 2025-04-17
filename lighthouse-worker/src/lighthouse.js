const chromeLauncher = require("chrome-launcher");
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

    // Chrome'u başlat
    chrome = await chromeLauncher.launch({
      chromeFlags: [
        "--headless",
        "--no-sandbox",
        "--disable-gpu",
        "--disable-dev-shm-usage",
        "--disable-software-rasterizer",
      ],
    });

    // Lighthouse modülünü dinamik olarak içe aktar (ESM uyumlu)
    const lighthouse = await import("lighthouse");

    // Lighthouse ayarları ve kategorilerini belirle
    const categories =
      analysisType === "detailed"
        ? ["performance", "accessibility", "best-practices", "seo"]
        : ["performance"];

    const opts = {
      logLevel: "info",
      output: "json",
      onlyCategories: categories,
      port: chrome.port,
    };

    // Lighthouse'u çalıştır (default export kullan)
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
