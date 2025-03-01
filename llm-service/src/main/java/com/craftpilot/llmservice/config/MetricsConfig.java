@Configuration
public class MetricsConfig {
    @Bean
    public MeterRegistry meterRegistry() {
        return new SimpleMeterRegistry();
    }

    @Bean
    public Timer.Builder requestLatencyTimerBuilder(MeterRegistry registry) {
        return Timer.builder("llm.request.latency")
            .description("Latency of LLM requests")
            .publishPercentileHistogram()
            .register(registry);
    }
}
