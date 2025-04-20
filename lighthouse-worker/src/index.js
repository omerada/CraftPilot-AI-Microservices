const express = require("express");
const { createClient } = require("redis");
const { runLighthouseAnalysis } = require("./lighthouse");
const { logger } = require("./logger");
require("dotenv").config();

// Redis bağlantı bilgileri
const REDIS_HOST = process.env.REDIS_HOST || "redis";
const REDIS_PORT = process.env.REDIS_PORT || 6379;
const REDIS_PASSWORD = process.env.REDIS_PASSWORD || "13579ada";

// Redis kuyruk ve sonuç key'leri
const QUEUE_NAME = process.env.LIGHTHOUSE_QUEUE_NAME || "lighthouse-jobs";
const RESULTS_PREFIX =
  process.env.LIGHTHOUSE_RESULTS_PREFIX || "lighthouse-results:";

// Worker ayarları
const POLL_INTERVAL = parseInt(process.env.POLL_INTERVAL || "5000");
const MAX_RETRIES = parseInt(process.env.MAX_RETRIES || "3");

// Redis istemcisi oluştur
const redisClient = createClient({
  url: `redis://:${REDIS_PASSWORD}@${REDIS_HOST}:${REDIS_PORT}`,
});

// Express uygulaması tanımla (health check endpoint'i için)
const app = express();
const PORT = process.env.PORT || 8086;

// Health check endpoint'i
app.get("/health", (req, res) => {
  res.json({ status: "UP", service: "lighthouse-worker" });
});

app.get("/", (req, res) => {
  res.json({
    service: "Lighthouse Worker Service",
    version: "1.0.0",
    status: "running",
  });
});

// Ana işlev: Redis'ten işleri izle ve işle
async function processJobs() {
  try {
    // Kuyruktan iş al (BLPOP kullanarak bekle)
    const result = await redisClient.lPop(QUEUE_NAME);

    if (result) {
      const job = JSON.parse(result);
      const { id: jobId, url, options = {} } = job;

      logger.info(`Processing job ${jobId} for URL: ${url}`);

      // İş durumunu PROCESSING olarak güncelle
      await updateJobStatus(jobId, "PROCESSING");

      try {
        // Lighthouse analizi çalıştır
        const analysisType = options.analysisType || "basic";
        const deviceType = options.deviceType || "desktop"; // Cihaz tipi parametresi ekle

        const startTime = Date.now();
        const results = await runLighthouseAnalysis(
          url,
          analysisType,
          deviceType
        );
        const duration = Date.now() - startTime;

        logger.info(
          `Analysis completed for job ${jobId} in ${duration}ms for device: ${deviceType}`
        );

        // Sonuçları işle ve kaydet
        results.analysisType = analysisType;
        results.deviceType = deviceType; // Sonuçlara deviceType ekle
        results.url = url;
        results.timestamp = Date.now();

        // Sonuçları Redis'e kaydet
        await redisClient.set(
          `${RESULTS_PREFIX}${jobId}`,
          JSON.stringify(results),
          { EX: 86400 }
        ); // 24 saat sakla

        // İş durumunu COMPLETED olarak güncelle
        await updateJobStatus(jobId, "COMPLETED");
        logger.info(`Job ${jobId} completed successfully`);
      } catch (error) {
        logger.error(`Error processing job ${jobId}: ${error.message}`);

        // İş durumunu FAILED olarak güncelle
        await updateJobStatus(jobId, "FAILED", error.message);
      }
    }
  } catch (error) {
    logger.error(`Error in job processing loop: ${error.message}`);
  } finally {
    // Kısa bir gecikme sonra tekrar dene
    setTimeout(processJobs, POLL_INTERVAL);
  }
}

// İş durumunu güncelleme yardımcı fonksiyonu
async function updateJobStatus(jobId, status, errorMessage = null) {
  const jobStatus = {
    jobId,
    complete: status === "COMPLETED" || status === "FAILED",
    status,
    error: errorMessage,
    timestamp: Date.now(),
  };

  try {
    await redisClient.set(
      `${RESULTS_PREFIX}status:${jobId}`,
      JSON.stringify(jobStatus),
      { EX: 1800 }
    ); // 30 dakika
    return true;
  } catch (error) {
    logger.error(`Failed to update job ${jobId} status: ${error.message}`);
    return false;
  }
}

// Redis'e bağlan ve uygulamayı başlat
async function startup() {
  try {
    await redisClient.connect();
    logger.info("Connected to Redis");

    // Express sunucusunu başlat
    app.listen(PORT, () => {
      logger.info(`Health check server running on port ${PORT}`);
    });

    // İş işleme döngüsünü başlat
    processJobs();
  } catch (error) {
    logger.error(`Startup error: ${error.message}`);
    process.exit(1);
  }
}

// SIGTERM ve SIGINT sinyallerini ele al
process.on("SIGTERM", async () => {
  logger.info("SIGTERM received, shutting down...");
  await redisClient.disconnect();
  process.exit(0);
});

process.on("SIGINT", async () => {
  logger.info("SIGINT received, shutting down...");
  await redisClient.disconnect();
  process.exit(0);
});

// Uygulamayı başlat
startup();
