// MongoDB başlangıç yapılandırma dosyası
db = db.getSiblingDB("admin");

// Uygulama veritabanı oluşturma
db = db.getSiblingDB("analytics");

// Koleksiyonlar oluşturma
db.createCollection("usage_metrics");
db.createCollection("performance_metrics");
db.createCollection("analytics_reports");

// Uygulama için ayrı kullanıcı oluşturma (daha kısıtlı yetkilerle)
db = db.getSiblingDB("admin");
db.createUser({
  user: "craftpilot",
  pwd: "secure_password",
  roles: [{ role: "readWrite", db: "analytics" }],
});

// Kritik koleksiyonlar için indeksler oluşturma
db = db.getSiblingDB("analytics");

// Usage Metrics koleksiyonu için indeksler
db.usage_metrics.createIndex({ userId: 1 }, { background: true });
db.usage_metrics.createIndex({ serviceType: 1 }, { background: true });
db.usage_metrics.createIndex({ modelId: 1 }, { background: true });
db.usage_metrics.createIndex({ serviceId: 1 }, { background: true });
db.usage_metrics.createIndex({ startTime: -1 }, { background: true });
db.usage_metrics.createIndex({ endTime: -1 }, { background: true });
db.usage_metrics.createIndex({ createdAt: -1 }, { background: true });

// Performance Metrics koleksiyonu için indeksler
db.performance_metrics.createIndex({ modelId: 1 }, { background: true });
db.performance_metrics.createIndex({ serviceId: 1 }, { background: true });
db.performance_metrics.createIndex({ type: 1 }, { background: true });
db.performance_metrics.createIndex({ timestamp: -1 }, { background: true });
db.performance_metrics.createIndex({ createdAt: -1 }, { background: true });

// Analytics Reports koleksiyonu için indeksler
db.analytics_reports.createIndex({ type: 1 }, { background: true });
db.analytics_reports.createIndex({ status: 1 }, { background: true });
db.analytics_reports.createIndex({ createdBy: 1 }, { background: true });
db.analytics_reports.createIndex({ reportStartTime: -1 }, { background: true });
db.analytics_reports.createIndex({ reportEndTime: -1 }, { background: true });
db.analytics_reports.createIndex({ createdAt: -1 }, { background: true });
db.analytics_reports.createIndex({ tags: 1 }, { background: true });

print("MongoDB başlatma işlemi tamamlandı");
