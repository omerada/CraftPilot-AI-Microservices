package com.craftpilot.codeservice.controller;

import com.craftpilot.codeservice.model.Code;
import com.craftpilot.codeservice.service.CodeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/codes")
@RequiredArgsConstructor
@Tag(name = "Code", description = "Code generation and management endpoints")
public class CodeController {
    private final CodeService codeService;

    @PostMapping
    @Operation(summary = "Generate code", description = "Generates code based on the provided prompt, language, and framework")
    public Mono<ResponseEntity<Code>> generateCode(@RequestBody Code code) {
        return codeService.generateCode(code)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get code by ID", description = "Retrieves code by its unique identifier")
    public Mono<ResponseEntity<Code>> getCode(@PathVariable String id) {
        return codeService.getCode(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get user's codes", description = "Retrieves all codes created by a specific user")
    public Flux<Code> getUserCodes(@PathVariable String userId) {
        return codeService.getUserCodes(userId);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete code", description = "Deletes code by its unique identifier")
    public Mono<ResponseEntity<Void>> deleteCode(@PathVariable String id) {
        return codeService.deleteCode(id)
                .then(Mono.just(ResponseEntity.ok().<Void>build()))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }
} 