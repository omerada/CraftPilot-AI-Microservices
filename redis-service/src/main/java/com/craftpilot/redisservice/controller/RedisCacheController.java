package com.craftpilot.redisservice.controller;

import com.craftpilot.redisservice.service.RedisCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/cache")
@RequiredArgsConstructor
public class RedisCacheController {

    private final RedisCacheService cacheService;

    @PostMapping("/{key}")
    public ResponseEntity<Void> setValue(
            @PathVariable String key,
            @RequestBody Object value,
            @RequestParam(required = false, defaultValue = "3600") long ttlSeconds) {
        cacheService.set(key, value, Duration.ofSeconds(ttlSeconds));
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{key}")
    public ResponseEntity<Object> getValue(@PathVariable String key) {
        return cacheService.get(key, Object.class)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{key}")
    public ResponseEntity<Void> deleteValue(@PathVariable String key) {
        cacheService.delete(key);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/pattern/{pattern}")
    public ResponseEntity<Void> deleteByPattern(@PathVariable String pattern) {
        cacheService.deletePattern(pattern);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/keys/{pattern}")
    public ResponseEntity<Set<String>> getKeys(@PathVariable String pattern) {
        return ResponseEntity.ok(cacheService.getKeys(pattern));
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        return ResponseEntity.ok(cacheService.getStats());
    }
} 