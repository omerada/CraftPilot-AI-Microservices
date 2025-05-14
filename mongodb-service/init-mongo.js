// MongoDB başlangıç yapılandırma dosyası
db = db.getSiblingDB("admin");

// Veritabanları tanımlaması - tüm microservice'ler için ayrı veritabanı
const craftpilotDbs = [
  "craftpilot_user_db",
  "craftpilot_llm_db",
  "craftpilot_subscription_db",
  "craftpilot_image_db",
  "craftpilot_notification_db",
  "craftpilot_analytics_db",
  "craftpilot_admin_db",
  "craftpilot_activity_log_db",
  "craftpilot_user_memory_db",
  "craftpilot_credit_db",
];

// Her veritabanını oluştur ve temel koleksiyonları ayarla
craftpilotDbs.forEach((dbName) => {
  db = db.getSiblingDB(dbName);
  print(`Veritabanı oluşturuluyor: ${dbName}`);

  // Her veritabanı için temel koleksiyonlar
  db.createCollection("logs");

  // Servis özelindeki koleksiyonları oluştur
  if (dbName === "craftpilot_user_db") {
    db.createCollection("users");
    db.createCollection("user_settings");
    db.createCollection("user_activity");

    // Indeksler
    db.users.createIndex({ email: 1 }, { unique: true, background: true });
    db.users.createIndex({ uid: 1 }, { unique: true, background: true });
    db.users.createIndex({ createdAt: 1 }, { background: true });
    db.user_settings.createIndex(
      { userId: 1 },
      { unique: true, background: true }
    );
    db.user_activity.createIndex({ userId: 1 }, { background: true });
    db.user_activity.createIndex({ timestamp: 1 }, { background: true });
  } else if (dbName === "craftpilot_subscription_db") {
    db.createCollection("subscriptions");
    db.createCollection("plans");
    db.createCollection("payment_history");

    // Indeksler
    db.subscriptions.createIndex({ userId: 1 }, { background: true });
    db.subscriptions.createIndex({ status: 1 }, { background: true });
    db.subscriptions.createIndex({ expiresAt: 1 }, { background: true });
    db.payment_history.createIndex({ userId: 1 }, { background: true });
    db.payment_history.createIndex({ timestamp: 1 }, { background: true });
  } else if (dbName === "craftpilot_credit_db") {
    db.createCollection("credits");
    db.createCollection("credit_transactions");

    // Indeksler
    db.credits.createIndex({ userId: 1 }, { background: true });
    db.credits.createIndex({ createdAt: 1 }, { background: true });
    db.credit_transactions.createIndex({ userId: 1 }, { background: true });
    db.credit_transactions.createIndex({ timestamp: 1 }, { background: true });
  } else if (dbName === "craftpilot_analytics_db") {
    db.createCollection("analytics");
    db.createCollection("metrics");
    db.createCollection("usage_data");

    // Indeksler
    db.analytics.createIndex({ userId: 1 }, { background: true });
    db.analytics.createIndex({ eventDate: 1 }, { background: true });
    db.analytics.createIndex({ eventType: 1 }, { background: true });
    db.metrics.createIndex({ timestamp: 1 }, { background: true });
    db.usage_data.createIndex({ userId: 1 }, { background: true });
    db.usage_data.createIndex({ date: 1 }, { background: true });
  } else if (dbName === "craftpilot_user_memory_db") {
    db.createCollection("user_memories");
    db.createCollection("user_instructions");
    db.createCollection("response_preferences");

    // Indeksler
    db.user_memories.createIndex({ userId: 1 }, { background: true });
    db.user_memories.createIndex({ timestamp: 1 }, { background: true });
    db.user_instructions.createIndex({ userId: 1 }, { background: true });
    db.response_preferences.createIndex({ userId: 1 }, { background: true });
  } else if (dbName === "craftpilot_activity_log_db") {
    db.createCollection("activity_events");
    db.createCollection("audit_logs");

    // Indeksler
    db.activity_events.createIndex({ userId: 1 }, { background: true });
    db.activity_events.createIndex({ timestamp: 1 }, { background: true });
    db.activity_events.createIndex({ eventType: 1 }, { background: true });
    db.audit_logs.createIndex({ timestamp: 1 }, { background: true });
  } else if (dbName === "craftpilot_llm_db") {
    db.createCollection("prompts");
    db.createCollection("completions");
    db.createCollection("models");

    // Indeksler
    db.prompts.createIndex({ userId: 1 }, { background: true });
    db.prompts.createIndex({ timestamp: 1 }, { background: true });
    db.completions.createIndex({ promptId: 1 }, { background: true });
    db.completions.createIndex({ userId: 1 }, { background: true });
    db.models.createIndex({ name: 1 }, { background: true });
  } else if (dbName === "craftpilot_image_db") {
    db.createCollection("images");
    db.createCollection("generations");

    // Indeksler
    db.images.createIndex({ userId: 1 }, { background: true });
    db.images.createIndex({ timestamp: 1 }, { background: true });
    db.generations.createIndex({ userId: 1 }, { background: true });
  } else if (dbName === "craftpilot_notification_db") {
    db.createCollection("notifications");
    db.createCollection("delivery_status");

    // Indeksler
    db.notifications.createIndex({ userId: 1 }, { background: true });
    db.notifications.createIndex({ timestamp: 1 }, { background: true });
    db.notifications.createIndex({ read: 1 }, { background: true });
    db.delivery_status.createIndex({ notificationId: 1 }, { background: true });
  } else if (dbName === "craftpilot_admin_db") {
    db.createCollection("admin_users");
    db.createCollection("system_config");
    db.createCollection("audit_trail");

    // Indeksler
    db.admin_users.createIndex(
      { email: 1 },
      { unique: true, background: true }
    );
    db.admin_users.createIndex(
      { username: 1 },
      { unique: true, background: true }
    );
    db.audit_trail.createIndex({ timestamp: 1 }, { background: true });
    db.audit_trail.createIndex({ adminId: 1 }, { background: true });
  }

  // Her veritabanı için log TTL indeksi oluştur (30 gün)
  db.logs.createIndex(
    { timestamp: 1 },
    { expireAfterSeconds: 2592000, background: true }
  );

  print(`Veritabanı yapılandırıldı: ${dbName}`);
});

// Uygulama için ayrı kullanıcı oluşturma (tüm veritabanlarına erişim)
db = db.getSiblingDB("admin");
db.createUser({
  user: "application_user",
  pwd: "app_secure_password",
  roles: craftpilotDbs.map((dbName) => ({ role: "readWrite", db: dbName })),
});

// Genel amaçlı eski veritabanı (geriye dönük uyumluluk için)
db = db.getSiblingDB("craftpilot");
db.createCollection("legacy_data");

print(
  "MongoDB başlatma işlemi tamamlandı - Tüm veritabanları ve kullanıcılar yapılandırıldı"
);
