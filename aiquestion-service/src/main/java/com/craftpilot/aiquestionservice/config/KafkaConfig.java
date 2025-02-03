package com.craftpilot.aiquestionservice.config;

import com.craftpilot.aiquestionservice.event.QuestionEvent;
import com.craftpilot.aiquestionservice.model.Question; 
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.messaging.Message;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import org.apache.kafka.clients.admin.NewTopic;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Slf4j
@EnableKafka
@Configuration
@RequiredArgsConstructor
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    @Value("${spring.kafka.producer.retries}")
    private int retries;

    @Value("${spring.kafka.producer.batch-size}")
    private int batchSize;

    @Value("${spring.kafka.producer.buffer-memory}")
    private int bufferMemory;

    @Value("${spring.cloud.stream.kafka.binder.configuration.security.protocol:PLAINTEXT}")
    private String securityProtocol;

    @Value("${spring.cloud.stream.kafka.binder.configuration.sasl.mechanism:PLAIN}")
    private String saslMechanism;

    @Value("${spring.cloud.stream.kafka.binder.configuration.sasl.jaas.config:}")
    private String jaasConfig;

    private final ObjectMapper objectMapper; 

    @Bean
    public ProducerFactory<String, QuestionEvent> producerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public KafkaTemplate<String, QuestionEvent> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    @Bean
    public ConsumerFactory<String, QuestionEvent> consumerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        config.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        return new DefaultKafkaConsumerFactory<>(config, new StringDeserializer(),
                new JsonDeserializer<>(QuestionEvent.class, objectMapper, false));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, QuestionEvent> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, QuestionEvent> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        return factory;
    }

    @Bean
    public KafkaTemplate<String, Question> questionKafkaTemplate() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        ProducerFactory<String, Question> factory = new DefaultKafkaProducerFactory<>(config);
        return new KafkaTemplate<>(factory);
    }

    @Bean
    public ConsumerFactory<String, Question> questionConsumerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        config.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        return new DefaultKafkaConsumerFactory<>(config, new StringDeserializer(),
                new JsonDeserializer<>(Question.class, objectMapper, false));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Question> questionKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Question> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(questionConsumerFactory());
        return factory;
    }

    @Bean
    public NewTopic questionEventsTopic() {
        return new NewTopic("question-events", 3, (short) 1);
    }

    @Bean
    public Sinks.Many<Message<QuestionEvent>> questionEventSink() {
        return Sinks.many().multicast().onBackpressureBuffer();
    }

    @Bean
    public Supplier<Flux<Message<QuestionEvent>>> questionEventProducer(Sinks.Many<Message<QuestionEvent>> sink) {
        return () -> sink.asFlux()
                .doOnNext(m -> {
                    QuestionEvent event = m.getPayload();
                    System.out.println("Producing question event: " + event.getEventType() + " for question: " + event.getQuestionId());
                });
    }

    @Bean
    public Consumer<Message<QuestionEvent>> questionEventConsumer() {
        return message -> {
            QuestionEvent event = message.getPayload();
            System.out.println("Consuming question event: " + event.getEventType() + " for question: " + event.getQuestionId());
        };
    }

    public void sendQuestionEvent(QuestionEvent event) {
        log.info("Sending question event: {} for question ID: {}", event.getEventType(), event.getQuestionId());
        kafkaTemplate().send("question-events", event.getQuestionId(), event);
    }

    public void sendQuestion(Question question) {
        log.info("Sending question event: {} for question ID: {}", question.getStatus(), question.getId());
        questionKafkaTemplate().send("questions", question.getId(), question);
    }
} 