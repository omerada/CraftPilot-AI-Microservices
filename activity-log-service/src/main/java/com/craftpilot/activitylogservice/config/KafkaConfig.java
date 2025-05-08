package com.craftpilot.activitylogservice.config;

import com.craftpilot.commons.activity.model.ActivityEvent;
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
    private String topic;

    @Bean
    public KafkaReceiver<String, ActivityEvent> activityEventReceiver() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.craftpilot.commons.activity.model,com.craftpilot.activitylogservice.model");
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, "com.craftpilot.commons.activity.model.ActivityEvent");
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        
        ReceiverOptions<String, ActivityEvent> receiverOptions = ReceiverOptions.<String, ActivityEvent>create(props)
                .subscription(Collections.singleton(topic))
                .addAssignListener(partitions -> log.info("Assigned: {}", partitions))
                .addRevokeListener(partitions -> log.info("Revoked: {}", partitions));
                
        return KafkaReceiver.create(receiverOptions);
    }
}
