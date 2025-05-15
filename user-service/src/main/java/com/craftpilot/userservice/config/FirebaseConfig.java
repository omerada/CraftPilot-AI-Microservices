package com.craftpilot.userservice.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import jakarta.annotation.PostConstruct;

/**
 * This is a stub Firebase configuration class.
 * Firebase functionality has been removed from this service.
 */
@Configuration
public class FirebaseConfig {
    private static final Logger logger = LoggerFactory.getLogger(FirebaseConfig.class);

    @PostConstruct
    public void initialize() {
        logger.info("Firebase functionality has been disabled in this service");
    }
}
