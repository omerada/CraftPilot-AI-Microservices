package com.craftpilot.llmservice.config;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.Getter;

import java.util.List;

public class MongoMetricsRegistry {
    @Getter
    private final String databaseName;
    private final List<String> collections;
    private final MeterRegistry meterRegistry;

    public MongoMetricsRegistry(String databaseName, List<String> collections, MeterRegistry meterRegistry) {
        this.databaseName = databaseName;
        this.collections = collections;
        this.meterRegistry = meterRegistry;
    }
}
