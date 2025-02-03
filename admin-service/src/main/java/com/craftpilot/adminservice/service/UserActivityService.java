package com.craftpilot.adminservice.service;

import com.craftpilot.adminservice.model.UserActivity;
import com.craftpilot.adminservice.repository.UserActivityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserActivityService {
    private final UserActivityRepository userActivityRepository;

    public Mono<UserActivity> recordActivity(UserActivity activity) {
        log.info("Recording user activity for user: {}", activity.getUserId());
        return userActivityRepository.save(activity);
    }

    public Mono<UserActivity> getActivityById(String id) {
        log.info("Retrieving user activity with ID: {}", id);
        return userActivityRepository.findById(id);
    }

    public Flux<UserActivity> getUserActivities(String userId) {
        log.info("Retrieving activities for user: {}", userId);
        return userActivityRepository.findByUserId(userId);
    }

    public Flux<UserActivity> getActivitiesByType(UserActivity.ActivityType activityType) {
        log.info("Retrieving activities of type: {}", activityType);
        return userActivityRepository.findByActivityType(activityType);
    }

    public Flux<UserActivity> getActivitiesByStatus(UserActivity.ActivityStatus status) {
        log.info("Retrieving activities with status: {}", status);
        return userActivityRepository.findByStatus(status);
    }

    public Flux<UserActivity> getActivitiesByTimeRange(LocalDateTime start, LocalDateTime end) {
        log.info("Retrieving activities between {} and {}", start, end);
        return userActivityRepository.findByTimeRange(start, end);
    }

    public Mono<Void> deleteActivity(String id) {
        log.info("Deleting activity with ID: {}", id);
        return userActivityRepository.deleteById(id);
    }

    public Mono<List<UserActivity>> getSuspiciousActivities() {
        log.info("Retrieving suspicious activities");
        return userActivityRepository.findByStatus(UserActivity.ActivityStatus.SUSPICIOUS)
                .collectList();
    }

    public Mono<List<UserActivity>> getBlockedActivities() {
        log.info("Retrieving blocked activities");
        return userActivityRepository.findByStatus(UserActivity.ActivityStatus.BLOCKED)
                .collectList();
    }

    public Mono<UserActivity> updateActivityStatus(String id, UserActivity.ActivityStatus newStatus) {
        log.info("Updating activity status for ID {} to {}", id, newStatus);
        return userActivityRepository.findById(id)
                .flatMap(activity -> {
                    activity.setStatus(newStatus);
                    return userActivityRepository.save(activity);
                });
    }

    public Mono<List<UserActivity>> getRecentActivities(LocalDateTime since) {
        log.info("Retrieving recent activities since {}", since);
        return userActivityRepository.findByTimeRange(since, LocalDateTime.now())
                .collectList();
    }

    public Mono<Long> getActivityCount(String userId) {
        log.info("Counting activities for user: {}", userId);
        return userActivityRepository.findByUserId(userId)
                .count();
    }
} 