// MongoDB başlangıç yapılandırma dosyası
db = db.getSiblingDB("admin");

// Uygulama veritabanı oluşturma
db = db.getSiblingDB("craftpilot");

// Koleksiyonlar oluşturma
db.createCollection("users");
db.createCollection("subscriptions");
db.createCollection("credits");
db.createCollection("projects");
db.createCollection("analytics");
db.createCollection("logs");

// Uygulama için ayrı kullanıcı oluşturma (daha kısıtlı yetkilerle)
db = db.getSiblingDB("admin");
db.createUser({
  user: "application_user",
  pwd: "app_secure_password",
  roles: [{ role: "readWrite", db: "craftpilot" }],
});

// Kritik koleksiyonlar için indeksler oluşturma
db = db.getSiblingDB("craftpilot");

// Users koleksiyonu için indeksler
db.users.createIndex({ email: 1 }, { unique: true });
db.users.createIndex({ uid: 1 }, { unique: true });
db.users.createIndex({ createdAt: 1 });

// Subscriptions koleksiyonu için indeksler
db.subscriptions.createIndex({ userId: 1 });
db.subscriptions.createIndex({ status: 1 });
db.subscriptions.createIndex({ expiresAt: 1 });

// Credits koleksiyonu için indeksler
db.credits.createIndex({ userId: 1 });
db.credits.createIndex({ createdAt: 1 });

// Projects koleksiyonu için indeksler
db.projects.createIndex({ userId: 1 });
db.projects.createIndex({ createdAt: 1 });

// Log koleksiyonu için TTL indeksi (30 gün sonra otomatik silme)
db.logs.createIndex({ timestamp: 1 }, { expireAfterSeconds: 2592000 });

// Analytics koleksiyonu için indeksler
db.analytics.createIndex({ userId: 1 });
db.analytics.createIndex({ eventDate: 1 });
db.analytics.createIndex({ eventType: 1 });

print("MongoDB başlatma işlemi tamamlandı");
