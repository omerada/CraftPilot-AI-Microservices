package com.craftpilot.activitylogservice.config;

import com.craftpilot.activitylogservice.model.ActivityEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverOptions;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
@Slf4j
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${kafka.consumer.group-id}")
    private String groupId;

    @Value("${kafka.consumer.auto-offset-reset:earliest}")
    private String autoOffsetReset;

    @Value("${kafka.topics.user-activity}")
    private String userActivityTopic;

    @Bean
    public ReceiverOptions<String, ActivityEvent> kafkaReceiverOptions() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, autoOffsetReset);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.craftpilot.*");
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, ActivityEvent.class.getName());

        return ReceiverOptions.<String, ActivityEvent>create(props)
                .subscription(Collections.singleton(userActivityTopic))
                .addAssignListener(partitions -> log.info("Assigned partitions: {}", partitions))
                .addRevokeListener(partitions -> log.info("Revoked partitions: {}", partitions));
    }

    @Bean
    public KafkaReceiver<String, ActivityEvent> kafkaReceiver(ReceiverOptions<String, ActivityEvent> receiverOptions) {
        return KafkaReceiver.create(receiverOptions);
    }
}
