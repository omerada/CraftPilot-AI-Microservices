package com.craftpilot.analyticsservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(properties = {
    "spring.data.mongodb.auto-index-creation=false",
    "mongodb.indexes.creation.enabled=false",
    "kafka.enabled=false"
})
class AnalyticsServiceApplicationTests {

    @DynamicPropertySource
    static void mongoDbProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", () -> "mongodb://localhost:27017/analytics-test");
        // Disable actual MongoDB operations in tests
        registry.add("spring.mongodb.embedded.enabled", () -> "true");
    }

    @Test
    void contextLoads() {
        // Just test that the application context loads successfully
    }
}