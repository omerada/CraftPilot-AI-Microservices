package com.craftpilot.userservice.controller;
 
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import com.craftpilot.userservice.model.UserPreferenceResponse;
import com.craftpilot.userservice.service.UserPreferenceService;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserPreferenceController {

    private final UserPreferenceService preferenceService;

    @GetMapping("/{userId}/preferences")
    public Mono<UserPreferenceResponse> getUserPreferences(@PathVariable String userId) {
        return preferenceService.getUserPreferences(userId)
                .subscribeOn(Schedulers.boundedElastic());
    }
} 