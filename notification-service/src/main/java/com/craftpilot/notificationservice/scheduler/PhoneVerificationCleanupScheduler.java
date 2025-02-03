package com.craftpilot.notificationservice.scheduler;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
@Slf4j
public class PhoneVerificationCleanupScheduler {

    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final MeterRegistry meterRegistry;
    private static final String PHONE_VERIFICATION_KEY_PREFIX = "phone:verification:";

    @Scheduled(cron = "${notification.phone-verification.cleanup.cron:0 */15 * * * *}")
    public void cleanupExpiredCodes() {
        log.info("Starting phone verification code cleanup");
        
        redisTemplate.keys(PHONE_VERIFICATION_KEY_PREFIX + "*")
                .flatMap(key -> redisTemplate.opsForValue().getAndDelete(key)
                        .doOnSuccess(value -> {
                            if (value != null) {
                                meterRegistry.counter("phone.verification.code.cleaned").increment();
                                log.debug("Cleaned up expired code for key: {}", key);
                            }
                        }))
                .doOnComplete(() -> {
                    log.info("Completed phone verification code cleanup");
                    meterRegistry.counter("phone.verification.cleanup.completed").increment();
                })
                .subscribe();
    }
} 