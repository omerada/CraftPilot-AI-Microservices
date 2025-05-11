package com.craftpilot.analyticsservice.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.domain.Sort;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;

import com.mongodb.MongoSocketException;
import com.mongodb.MongoSocketOpenException;
import com.mongodb.MongoTimeoutException;
import com.mongodb.ServerAddress;
import com.mongodb.connection.ServerSettings;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import java.time.Duration;
import java.net.InetAddress;
import java.net.UnknownHostException;

@Configuration
@Slf4j
@EnableAsync
@ConditionalOnProperty(name = "mongodb.indexes.creation.enabled", havingValue = "true", matchIfMissing = true)
public class MongoIndexConfig {

    private final ReactiveMongoTemplate mongoTemplate;
    private boolean indexesInitialized = false;

    @Value("${mongodb.indexes.creation.enabled:true}")
    private boolean indexCreationEnabled;

    @Value("${mongodb.indexes.creation.retry.max-attempts:5}")
    private int maxRetryAttempts;

    @Value("${mongodb.indexes.creation.retry.delay:10000}")
    private long retryDelayMs;

    @Value("${mongodb.indexes.creation.retry.max-delay:60000}")
    private long maxRetryDelayMs;
    
    @Value("${mongodb.indexes.creation.startup-fail-fast:false}")
    private boolean startupFailFast;
    
    @Value("${spring.data.mongodb.uri}")
    private String mongoUri;

    @Autowired
    public MongoIndexConfig(ReactiveMongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @EventListener(ContextRefreshedEvent.class)
    public void initIndexesAfterStartup() {
        if (!indexCreationEnabled) {
            log.info("MongoDB index creation is disabled by configuration");
            return;
        }

        if (indexesInitialized) {
            log.info("MongoDB indexes are already initialized");
            return;
        }
        
        // MongoDB URI'dan host adresini çıkar ve bağlantı kontrolü yap
        try {
            String host = extractHostFromUri(mongoUri);
            if (host != null) {
                log.info("Testing connection to MongoDB host: {}", host);
                boolean hostResolvable = isHostResolvable(host);
                log.info("MongoDB host resolution test: {}", hostResolvable ? "SUCCESS" : "FAILED");
                if (!hostResolvable) {
                    log.warn("MongoDB host cannot be resolved. This may cause connection problems.");
                }
            }
        } catch (Exception e) {
            log.warn("Could not extract or validate MongoDB host: {}", e.getMessage());
        }
        
        // Indeks oluşturmayı async olarak başlat
        asyncCreateIndexes(0);
    }
    
    private String extractHostFromUri(String uri) {
        try {
            // mongodb://username:password@host:port/database
            if (uri != null && uri.contains("@")) {
                String hostPart = uri.split("@")[1];
                if (hostPart.contains("/")) {
                    hostPart = hostPart.split("/")[0];
                }
                if (hostPart.contains(":")) {
                    return hostPart.split(":")[0];
                }
                return hostPart;
            }
        } catch (Exception e) {
            log.warn("Error extracting host from MongoDB URI: {}", e.getMessage());
        }
        return null;
    }
    
    private boolean isHostResolvable(String host) {
        try {
            InetAddress.getByName(host);
            return true;
        } catch (UnknownHostException e) {
            return false;
        }
    }

    @Async
    protected void asyncCreateIndexes(int attempt) {
        if (attempt >= maxRetryAttempts) {
            log.error("Failed to create MongoDB indexes after {} attempts. Giving up.", maxRetryAttempts);
            return;
        }

        if (attempt > 0) {
            long delay = Math.min(retryDelayMs * attempt, maxRetryDelayMs);
            log.info("Retrying MongoDB index creation (attempt {}/{}). Waiting for {} ms...", 
                attempt + 1, maxRetryAttempts, delay);
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Index creation retry interrupted");
                return;
            }
        }

        log.info("Creating MongoDB indexes (attempt {}/{})", attempt + 1, maxRetryAttempts);
        
        try {
            // MongoDB bağlantısını test et
            boolean isConnected = testConnection();
            
            if (!isConnected) {
                log.warn("Cannot connect to MongoDB. Will retry index creation later.");
                asyncCreateIndexes(attempt + 1);
                return;
            }
            
            // UsageMetrics indeksleri
            createUsageMetricsIndexes();
            
            // PerformanceMetrics indeksleri
            createPerformanceMetricsIndexes();
            
            // AnalyticsReport indeksleri
            createAnalyticsReportIndexes();
            
            log.info("MongoDB indexes created successfully");
            indexesInitialized = true;
        } catch (Exception e) {
            log.error("Failed to create MongoDB indexes: {}", e.getMessage(), e);
            asyncCreateIndexes(attempt + 1);
        }
    }

    private boolean testConnection() {
        try {
            return mongoTemplate.collectionExists("test_connection")
                .timeout(Duration.ofSeconds(5))
                .onErrorReturn(false)
                .block();
        } catch (Exception e) {
            log.warn("MongoDB connection test failed: {}", e.getMessage());
            return false;
        }
    }

    @Retryable(
        value = {MongoSocketException.class, MongoTimeoutException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    private void createUsageMetricsIndexes() {
        try {
            mongoTemplate.indexOps("usage_metrics").ensureIndex(
                    new Index().on("userId", Sort.Direction.ASC))
                    .timeout(Duration.ofSeconds(5))
                    .block();
            log.info("Created userId index for usage_metrics");
                
            mongoTemplate.indexOps("usage_metrics").ensureIndex(
                    new Index().on("serviceType", Sort.Direction.ASC))
                    .timeout(Duration.ofSeconds(5))
                    .block();
            log.info("Created serviceType index for usage_metrics");
                
            mongoTemplate.indexOps("usage_metrics").ensureIndex(
                    new Index().on("startTime", Sort.Direction.ASC).on("endTime", Sort.Direction.ASC))
                    .timeout(Duration.ofSeconds(5))
                    .block();
            log.info("Created time range index for usage_metrics");
        } catch (Exception e) {
            log.error("Error creating indexes for usage_metrics: {}", e.getMessage());
            throw e;
        }
    }

    @Retryable(
        value = {MongoSocketException.class, MongoTimeoutException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    private void createPerformanceMetricsIndexes() {
        try {
            mongoTemplate.indexOps("performance_metrics").ensureIndex(
                    new Index().on("modelId", Sort.Direction.ASC))
                    .timeout(Duration.ofSeconds(5))
                    .block();
            log.info("Created modelId index for performance_metrics");
                
            mongoTemplate.indexOps("performance_metrics").ensureIndex(
                    new Index().on("timestamp", Sort.Direction.DESC))
                    .timeout(Duration.ofSeconds(5))
                    .block();
            log.info("Created timestamp index for performance_metrics");
        } catch (Exception e) {
            log.error("Error creating indexes for performance_metrics: {}", e.getMessage());
            throw e;
        }
    }

    @Retryable(
        value = {MongoSocketException.class, MongoTimeoutException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    private void createAnalyticsReportIndexes() {
        try {
            mongoTemplate.indexOps("analytics_reports").ensureIndex(
                    new Index().on("createdBy", Sort.Direction.ASC))
                    .timeout(Duration.ofSeconds(5))
                    .block();
            log.info("Created createdBy index for analytics_reports");
                
            mongoTemplate.indexOps("analytics_reports").ensureIndex(
                    new Index().on("type", Sort.Direction.ASC).on("status", Sort.Direction.ASC))
                    .timeout(Duration.ofSeconds(5))
                    .block();
            log.info("Created type-status index for analytics_reports");
                
            mongoTemplate.indexOps("analytics_reports").ensureIndex(
                    new Index().on("reportStartTime", Sort.Direction.ASC).on("reportEndTime", Sort.Direction.ASC))
                    .timeout(Duration.ofSeconds(5))
                    .block();
            log.info("Created time range index for analytics_reports");
        } catch (Exception e) {
            log.error("Error creating indexes for analytics_reports: {}", e.getMessage());
            throw e;
        }
    }
}
