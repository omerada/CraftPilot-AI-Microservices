package com.craftpilot.activitylogservice.config;

import com.craftpilot.activitylogservice.model.ActivityEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverOptions;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Configuration
@Slf4j
@ConditionalOnProperty(name = "spring.kafka.bootstrap-servers")
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id:activity-log-service}")
    private String groupId;

    @Value("${activity.kafka.consumer.topic:user-activity}")
    private String activityTopic;

    @Value("${spring.kafka.consumer.auto-offset-reset:latest}")
    private String autoOffsetReset;

    @Bean
    public KafkaReceiver<String, ActivityEvent> kafkaReceiver() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, autoOffsetReset);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*"); // Daha güvenli bir yapılandırma için spesifik paketler tanımlanabilir
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, ActivityEvent.class.getName());
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false); // Manuel acknowledge kullanıyoruz

        ReceiverOptions<String, ActivityEvent> receiverOptions = ReceiverOptions
                .<String, ActivityEvent>create(props)
                .subscription(Collections.singleton(activityTopic))
                .addAssignListener(partitions -> 
                    log.info("Assigned: {}", partitions))
                .addRevokeListener(partitions -> 
                    log.info("Revoked: {}", partitions));

        return KafkaReceiver.create(receiverOptions);
    }
}
