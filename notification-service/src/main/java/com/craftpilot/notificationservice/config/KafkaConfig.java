package com.craftpilot.notificationservice.config;

import com.craftpilot.notificationservice.event.NotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import org.springframework.messaging.Message;
import org.springframework.beans.factory.annotation.Value;

import java.util.function.Consumer;
import java.util.function.Supplier;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class KafkaConfig {
    
    @Value("${spring.cloud.stream.kafka.binder.brokers}")
    private String bootstrapServers;

    @Bean
    public Sinks.Many<Message<NotificationEvent>> notificationEventSink() {
        return Sinks.many().multicast().onBackpressureBuffer();
    }

    @Bean
    public Supplier<Flux<Message<NotificationEvent>>> notificationEventProducer(
            Sinks.Many<Message<NotificationEvent>> sink) {
        return () -> sink.asFlux()
                .doOnNext(message -> log.info("Producing notification event: type={}, id={}, brokers={}", 
                        message.getPayload().getEventType(),
                        message.getPayload().getNotificationId(),
                        bootstrapServers));
    }

    @Bean
    public Consumer<Message<NotificationEvent>> notificationEventConsumer() {
        return message -> log.info("Consuming notification event: type={}, id={}, brokers={}", 
                message.getPayload().getEventType(),
                message.getPayload().getNotificationId(),
                bootstrapServers);
    }
}