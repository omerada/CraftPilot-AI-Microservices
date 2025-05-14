package com.craftpilot.subscriptionservice.service;

import com.craftpilot.subscriptionservice.event.SubscriptionEvent;
import com.craftpilot.subscriptionservice.exception.SubscriptionNotFoundException;
import com.craftpilot.subscriptionservice.model.subscription.entity.Subscription;
import com.craftpilot.subscriptionservice.model.subscription.enums.SubscriptionStatus;
import com.craftpilot.subscriptionservice.model.subscription.request.CreateSubscriptionRequest;
import com.craftpilot.subscriptionservice.repository.SubscriptionPlanRepository;
import com.craftpilot.subscriptionservice.repository.SubscriptionRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Slf4j
@Service
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final PaymentService paymentService;
    private final KafkaTemplate<String, SubscriptionEvent> kafkaTemplate;
    private final MeterRegistry meterRegistry;

    private Counter subscriptionCreationCounter;
    private Counter subscriptionRenewalCounter;
    private Counter subscriptionCancellationCounter;

    @Value("${kafka.topics.subscription-events}")
    private String subscriptionEventsTopic;

    public SubscriptionService(
            SubscriptionRepository subscriptionRepository,
            SubscriptionPlanRepository subscriptionPlanRepository,
            PaymentService paymentService,
            KafkaTemplate<String, SubscriptionEvent> kafkaTemplate,
            MeterRegistry meterRegistry) {
        this.subscriptionRepository = subscriptionRepository;
        this.subscriptionPlanRepository = subscriptionPlanRepository;
        this.paymentService = paymentService;
        this.kafkaTemplate = kafkaTemplate;
        this.meterRegistry = meterRegistry;
    }

    @PostConstruct
    private void initMetrics() {
        subscriptionCreationCounter = Counter.builder("subscription.creation")
                .description("Number of subscriptions created")
                .register(meterRegistry);

        subscriptionRenewalCounter = Counter.builder("subscription.renewal")
                .description("Number of subscriptions renewed")
                .register(meterRegistry);

        subscriptionCancellationCounter = Counter.builder("subscription.cancellation")
                .description("Number of subscriptions cancelled")
                .register(meterRegistry);
    }

    public Mono<Subscription> createSubscription(CreateSubscriptionRequest request) {
        return subscriptionPlanRepository.findById(request.getPlanId())
                .switchIfEmpty(Mono.error(new RuntimeException("Plan bulunamadı")))
                .flatMap(plan -> {
                    Subscription subscription = Subscription.builder()
                            .userId(request.getUserId())
                            .planId(plan.getId())
                            .amount(plan.getPrice())
                            .description(plan.getName())
                            .startDate(LocalDateTime.now())
                            .endDate(LocalDateTime.now().plus(plan.getDurationInDays(), ChronoUnit.DAYS))
                            .status(SubscriptionStatus.PENDING)
                            .active(false)
                            .deleted(false)
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .build();

                    return subscriptionRepository.save(subscription)
                            .flatMap(savedSubscription -> paymentService.createPayment(savedSubscription)
                                    .map(paymentResponse -> {
                                        savedSubscription.setPaymentUrl(paymentResponse.getPaymentUrl());
                                        return savedSubscription;
                                    }));
                });
    }

    @Cacheable(value = "subscriptions", key = "#id")
    public Mono<Subscription> getSubscription(String id) {
        return subscriptionRepository.findById(id)
                .switchIfEmpty(Mono.error(new SubscriptionNotFoundException("Subscription not found with id: " + id)))
                .doOnSuccess(subscription -> log.debug("Retrieved subscription: {}", subscription.getId()));
    }

    @Cacheable(value = "subscriptions", key = "'user:' + #userId")
    public Flux<Subscription> getUserSubscriptions(String userId) {
        return subscriptionRepository.findByUserId(userId)
                .doOnComplete(() -> log.debug("Retrieved all subscriptions for user: {}", userId));
    }

    @Cacheable(value = "subscriptions", key = "'active:' + #userId")
    public Mono<Subscription> getActiveSubscription(String userId) {
        return subscriptionRepository.findByUserIdAndIsActiveTrue(userId)
                .switchIfEmpty(Mono.error(new RuntimeException("Aktif abonelik bulunamadı")));
    }

    @CacheEvict(value = "subscriptions", key = "#id")
    public Mono<Subscription> cancelSubscription(String subscriptionId) {
        return subscriptionRepository.findById(subscriptionId)
                .switchIfEmpty(Mono.error(new RuntimeException("Abonelik bulunamadı")))
                .flatMap(subscription -> {
                    subscription.setStatus(SubscriptionStatus.CANCELLED);
                    subscription.setActive(false);
                    subscription.setDeleted(true);
                    subscription.setUpdatedAt(LocalDateTime.now());
                    return subscriptionRepository.save(subscription)
                            .doOnSuccess(s -> {
                                subscriptionCancellationCounter.increment();
                                sendSubscriptionEvent(s, "SUBSCRIPTION_CANCELLED");
                            });
                });
    }

    @CacheEvict(value = "subscriptions", key = "#id")
    public Mono<Subscription> renewSubscription(String id) {
        return subscriptionRepository.findById(id)
                .switchIfEmpty(Mono.error(new SubscriptionNotFoundException("Subscription not found with id: " + id)))
                .flatMap(subscription -> subscriptionPlanRepository.findById(subscription.getPlanId())
                        .flatMap(plan -> {
                            subscription.setStatus(SubscriptionStatus.ACTIVE);
                            subscription.setStartDate(LocalDateTime.now());
                            subscription.setEndDate(LocalDateTime.now().plusDays(plan.getDurationInDays()));

                            return subscriptionRepository.save(subscription)
                                    .doOnSuccess(renewedSubscription -> {
                                        subscriptionRenewalCounter.increment();
                                        sendSubscriptionEvent(renewedSubscription, "SUBSCRIPTION_RENEWED");
                                        log.info("Renewed subscription: {}", renewedSubscription.getId());
                                    });
                        }));
    }

    public Flux<Subscription> getExpiringSubscriptions() {
        LocalDateTime expirationThreshold = LocalDateTime.now().plusDays(7);
        return subscriptionRepository.findByEndDateBeforeAndIsActiveTrue(expirationThreshold)
                .doOnComplete(() -> log.debug("Retrieved all expiring subscriptions"));
    }

    public Flux<Subscription> getAllSubscriptions(String userId) {
        return subscriptionRepository.findByUserId(userId);
    }

    public Mono<Subscription> activateSubscription(String subscriptionId) {
        return subscriptionRepository.findById(subscriptionId)
                .switchIfEmpty(Mono.error(new RuntimeException("Abonelik bulunamadı")))
                .flatMap(subscription -> {
                    subscription.setStatus(SubscriptionStatus.ACTIVE);
                    subscription.setActive(true);
                    subscription.setUpdatedAt(LocalDateTime.now());
                    return subscriptionRepository.save(subscription)
                            .doOnSuccess(s -> {
                                subscriptionCreationCounter.increment();
                                sendSubscriptionEvent(s, "SUBSCRIPTION_ACTIVATED");
                            });
                });
    }

    @Scheduled(cron = "0 0 0 * * *") // Her gün gece yarısı
    public void checkExpiringSubscriptions() {
        LocalDateTime tomorrow = LocalDateTime.now().plusDays(1);

        subscriptionRepository.findByEndDateBeforeAndIsActiveTrue(tomorrow)
                .flatMap(subscription -> {
                    subscription.setStatus(SubscriptionStatus.EXPIRED);
                    subscription.setActive(false);
                    subscription.setUpdatedAt(LocalDateTime.now());
                    return subscriptionRepository.save(subscription)
                            .doOnSuccess(savedSubscription -> {
                                SubscriptionEvent event = SubscriptionEvent.builder()
                                        .subscriptionId(savedSubscription.getId())
                                        .userId(savedSubscription.getUserId())
                                        .eventType("SUBSCRIPTION_EXPIRED")
                                        .timestamp(LocalDateTime.now().toEpochSecond(java.time.ZoneOffset.UTC))
                                        .subscription(savedSubscription)
                                        .build();

                                kafkaTemplate.send(subscriptionEventsTopic, event);
                            });
                })
                .subscribe();
    }

    private void sendSubscriptionEvent(Subscription subscription, String eventType) {
        SubscriptionEvent event = SubscriptionEvent.builder()
                .eventType(eventType)
                .subscriptionId(subscription.getId())
                .userId(subscription.getUserId())
                .timestamp(System.currentTimeMillis())
                .subscription(subscription)
                .build();

        kafkaTemplate.send(subscriptionEventsTopic, subscription.getId(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to send subscription event: {}", event, ex);
                    } else {
                        log.debug("Sent subscription event: {}", event);
                    }
                });
    }
}