package com.craftpilot.aiquestionservice.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.micrometer.core.aop.TimedAspect;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;

@Configuration
public class MetricsConfig {

    private final JvmMemoryMetrics jvmMemoryMetrics;
    private final ProcessorMetrics processorMetrics;
    private final JvmGcMetrics jvmGcMetrics;

    public MetricsConfig() {
        this.jvmMemoryMetrics = new JvmMemoryMetrics();
        this.processorMetrics = new ProcessorMetrics();
        this.jvmGcMetrics = new JvmGcMetrics();
    }

    @Bean
    MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
        return registry -> {
            registry.config().commonTags("application", "aiquestion-service");
            
            // JVM metrics
            jvmMemoryMetrics.bindTo(registry);
            processorMetrics.bindTo(registry);
            jvmGcMetrics.bindTo(registry);
        };
    }

    @Bean
    public MeterRegistry meterRegistry() {
        return new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
    }

    @Bean
    public Counter questionCreatedCounter(MeterRegistry registry) {
        return Counter.builder("questions.created")
                .description("Number of questions created")
                .register(registry);
    }

    @Bean
    public Counter questionCompletedCounter(MeterRegistry registry) {
        return Counter.builder("questions.completed")
                .description("Number of questions completed")
                .register(registry);
    }

    @Bean
    public Counter questionFailedCounter(MeterRegistry registry) {
        return Counter.builder("questions.failed")
                .description("Number of questions failed")
                .register(registry);
    }

    @Bean
    public Counter modelCreatedCounter(MeterRegistry registry) {
        return Counter.builder("models.created")
                .description("Number of AI models created")
                .register(registry);
    }

    @Bean
    public Counter modelUpdatedCounter(MeterRegistry registry) {
        return Counter.builder("models.updated")
                .description("Number of AI models updated")
                .register(registry);
    }

    @Bean
    public Counter modelDeletedCounter(MeterRegistry registry) {
        return Counter.builder("models.deleted")
                .description("Number of AI models deleted")
                .register(registry);
    }

    @Bean
    public Timer questionProcessingTimer(MeterRegistry registry) {
        return Timer.builder("questions.processing.time")
                .description("Time taken to process questions")
                .register(registry);
    }

    @Bean
    public Timer aiProviderResponseTimer(MeterRegistry registry) {
        return Timer.builder("ai.provider.response.time")
                .description("Time taken by AI providers to respond")
                .register(registry);
    }

    @Bean
    public TimedAspect timedAspect(MeterRegistry registry) {
        return new TimedAspect(registry);
    }
} 