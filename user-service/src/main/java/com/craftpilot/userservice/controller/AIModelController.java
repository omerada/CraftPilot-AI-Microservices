package com.craftpilot.userservice.controller;

import com.craftpilot.userservice.dto.ai.ModelResponse;
import com.craftpilot.userservice.service.AIModelService;
import com.craftpilot.userservice.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "AI Models", description = "AI model yönetimi API'leri")
public class AIModelController {

    private final AIModelService modelService;
    private final UserService userService;

    @GetMapping("/models/available")
    @Operation(summary = "Kullanıcıya uygun AI modellerini getir", description = "Kullanıcının planına göre erişebileceği tüm AI modellerini getirir")
    public Mono<ResponseEntity<ModelResponse>> getAvailableModels() {
        return ReactiveSecurityContextHolder.getContext()
            .map(SecurityContext::getAuthentication)
            .map(Authentication::getName)
            .flatMap(userId -> {
                log.info("Kullanıcı için erişilebilir AI modelleri getiriliyor: userId={}", userId);
                return userService.getUserPlan(userId)
                    .flatMap(userPlan ->
                        userService.getUserPreferences(userId)
                            .flatMap(preferences -> {
                                // Son seçilen model yoksa varsayılan değer kullan
                                final String lastSelectedModel = preferences.getLastSelectedModelId() != null ?
                                        preferences.getLastSelectedModelId() : 
                                        modelService.getDefaultModelForPlan(userPlan);

                                return modelService.getAvailableModels(userPlan)
                                    .map(modelsData -> {
                                        ModelResponse response = ModelResponse.builder()
                                            .models(modelsData.getModels())
                                            .providers(modelsData.getProviders())
                                            .defaultModelId(modelService.getDefaultModelForPlan(userPlan))
                                            .userPlan(userPlan)
                                            .lastSelectedModelId(lastSelectedModel)
                                            .version(modelsData.getVersion())
                                            .build();
                                        return ResponseEntity.ok(response);
                                    });
                            })
                    )
                    .doOnSuccess(response -> log.info("AI modelleri başarıyla getirildi: userId={}", userId))
                    .doOnError(e -> log.error("AI modelleri getirilirken hata: userId={}, error={}", userId, e.getMessage()));
            });
    }
}
