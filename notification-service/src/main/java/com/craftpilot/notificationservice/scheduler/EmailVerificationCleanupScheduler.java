package com.craftpilot.notificationservice.scheduler;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;


@Slf4j
@Component
@RequiredArgsConstructor
public class EmailVerificationCleanupScheduler {

    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final MeterRegistry meterRegistry;
    private static final String EMAIL_VERIFICATION_KEY_PREFIX = "email:verification:";

    @Scheduled(cron = "${notification.email-verification.cleanup.cron:0 0 */6 * * *}")
    public void cleanupExpiredTokens() {
        log.info("Starting email verification token cleanup");
        
        redisTemplate.keys(EMAIL_VERIFICATION_KEY_PREFIX + "*")
                .flatMap(key -> redisTemplate.opsForValue().getAndDelete(key)
                        .doOnSuccess(value -> {
                            if (value != null) {
                                meterRegistry.counter("email.verification.token.cleaned").increment();
                                log.debug("Cleaned up expired token for key: {}", key);
                            }
                        }))
                .doOnComplete(() -> {
                    log.info("Completed email verification token cleanup");
                    meterRegistry.counter("email.verification.cleanup.completed").increment();
                })
                .subscribe();
    }
} 