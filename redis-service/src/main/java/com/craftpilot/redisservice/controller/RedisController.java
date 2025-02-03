package com.craftpilot.redisservice.controller;

import com.craftpilot.redisservice.service.RedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/redis")
@RequiredArgsConstructor
public class RedisController {

    private final RedisService redisService;

    @GetMapping("/{key}")
    public ResponseEntity<Object> getValue(@PathVariable String key) {
        Object value = redisService.get(key);
        return ResponseEntity.ok(value);
    }

    @PostMapping("/{key}")
    public ResponseEntity<Void> setValue(
            @PathVariable String key,
            @RequestBody Object value,
            @RequestParam(defaultValue = "3600") long timeoutSeconds) {
        redisService.set(key, value, timeoutSeconds);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{key}")
    public ResponseEntity<Void> deleteValue(@PathVariable String key) {
        redisService.delete(key);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/cache/{mapName}/{key}")
    public ResponseEntity<Object> getCacheValue(
            @PathVariable String mapName,
            @PathVariable String key) {
        Object value = redisService.getFromCache(mapName, key);
        return ResponseEntity.ok(value);
    }

    @PostMapping("/cache/{mapName}/{key}")
    public ResponseEntity<Void> setCacheValue(
            @PathVariable String mapName,
            @PathVariable String key,
            @RequestBody Object value,
            @RequestParam(defaultValue = "3600") long timeoutSeconds) {
        redisService.setWithCache(mapName, key, value, timeoutSeconds);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/lock/{key}")
    public ResponseEntity<Boolean> acquireLock(
            @PathVariable String key,
            @RequestParam(defaultValue = "1000") long waitTimeMs,
            @RequestParam(defaultValue = "5000") long leaseTimeMs) {
        boolean acquired = redisService.acquireLock(key, waitTimeMs, leaseTimeMs);
        return ResponseEntity.ok(acquired);
    }

    @DeleteMapping("/lock/{key}")
    public ResponseEntity<Void> releaseLock(@PathVariable String key) {
        redisService.releaseLock(key);
        return ResponseEntity.ok().build();
    }
} 