package com.craftpilot.notificationservice.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
        return registry -> {
            registry.config().commonTags("application", "notification-service");
            jvmMemoryMetrics.bindTo(registry);
            processorMetrics.bindTo(registry);
            jvmGcMetrics.bindTo(registry);
        };
    }

    @Bean
    public Counter notificationCreatedCounter(MeterRegistry registry) {
        return Counter.builder("notification.created")
                .description("Number of notifications created")
                .register(registry);
    }

    @Bean
    public Counter notificationSentCounter(MeterRegistry registry) {
        return Counter.builder("notification.sent")
                .description("Number of notifications sent")
                .register(registry);
    }

    @Bean
    public Counter notificationFailedCounter(MeterRegistry registry) {
        return Counter.builder("notification.failed")
                .description("Number of notifications failed")
                .register(registry);
    }

    @Bean
    public Timer notificationProcessingTimer(MeterRegistry registry) {
        return Timer.builder("notification.processing")
                .description("Time taken to process notifications")
                .register(registry);
    }

    @Bean
    public Timer emailSendingTimer(MeterRegistry registry) {
        return Timer.builder("notification.email.sending")
                .description("Time taken to send emails")
                .register(registry);
    }

    @Bean
    public Timer pushNotificationTimer(MeterRegistry registry) {
        return Timer.builder("notification.push.duration")
                .description("Time taken to send push notifications")
                .register(registry);
    }
} 