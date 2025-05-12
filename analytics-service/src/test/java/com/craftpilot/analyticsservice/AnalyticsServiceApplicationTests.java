package com.craftpilot.analyticsservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest; 

@SpringBootTest(properties = {
    "spring.cloud.gcp.firestore.project-id=craft-pilot-ai",
    "spring.cloud.gcp.credentials.location=classpath:firebase-service-account.json"
})
class AnalyticsServiceApplicationTests {

    @Test
    void contextLoads() {
    }
} 