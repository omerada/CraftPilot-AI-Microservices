@Configuration
public class LoggingConfig {
    @Bean
    public HttpTraceRepository httpTraceRepository() {
        return new InMemoryHttpTraceRepository();
    }

    @Bean
    public WebFilter loggingFilter() {
        return (exchange, chain) -> {
            long startTime = System.currentTimeMillis();
            log.info("Incoming request: {} {}", 
                exchange.getRequest().getMethod(), 
                exchange.getRequest().getURI());
            
            return chain.filter(exchange)
                .doFinally(signalType -> {
                    long endTime = System.currentTimeMillis();
                    log.info("Response for {} {} took {}ms with status {}", 
                        exchange.getRequest().getMethod(),
                        exchange.getRequest().getURI(),
                        endTime - startTime,
                        exchange.getResponse().getStatusCode());
                });
        };
    }
}
