package com.craftpilot.redisservice.controller;

import com.craftpilot.redisservice.service.RedisService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RedisController.class)
class RedisControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RedisService redisService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void getValue_ShouldReturnValue() throws Exception {
        // Given
        String key = "testKey";
        String value = "testValue";
        when(redisService.get(key)).thenReturn(value);

        // When & Then
        mockMvc.perform(get("/api/redis/{key}", key))
                .andExpect(status().isOk())
                .andExpect(content().string(value));

        verify(redisService).get(key);
    }

    @Test
    void setValue_ShouldSetValue() throws Exception {
        // Given
        String key = "testKey";
        String value = "testValue";
        long timeout = 3600;

        // When & Then
        mockMvc.perform(post("/api/redis/{key}", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(value))
                        .param("timeoutSeconds", String.valueOf(timeout)))
                .andExpect(status().isOk());

        verify(redisService).set(key, value, timeout);
    }

    @Test
    void deleteValue_ShouldDeleteValue() throws Exception {
        // Given
        String key = "testKey";

        // When & Then
        mockMvc.perform(delete("/api/redis/{key}", key))
                .andExpect(status().isOk());

        verify(redisService).delete(key);
    }

    @Test
    void getCacheValue_ShouldReturnCachedValue() throws Exception {
        // Given
        String mapName = "testMap";
        String key = "testKey";
        String value = "testValue";
        when(redisService.getFromCache(mapName, key)).thenReturn(value);

        // When & Then
        mockMvc.perform(get("/api/redis/cache/{mapName}/{key}", mapName, key))
                .andExpect(status().isOk())
                .andExpect(content().string(value));

        verify(redisService).getFromCache(mapName, key);
    }

    @Test
    void setCacheValue_ShouldSetCacheValue() throws Exception {
        // Given
        String mapName = "testMap";
        String key = "testKey";
        String value = "testValue";
        long timeout = 3600;

        // When & Then
        mockMvc.perform(post("/api/redis/cache/{mapName}/{key}", mapName, key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(value))
                        .param("timeoutSeconds", String.valueOf(timeout)))
                .andExpect(status().isOk());

        verify(redisService).setWithCache(mapName, key, value, timeout);
    }

    @Test
    void acquireLock_ShouldAcquireLock() throws Exception {
        // Given
        String key = "testLock";
        long waitTime = 1000;
        long leaseTime = 5000;
        when(redisService.acquireLock(key, waitTime, leaseTime)).thenReturn(true);

        // When & Then
        mockMvc.perform(post("/api/redis/lock/{key}", key)
                        .param("waitTimeMs", String.valueOf(waitTime))
                        .param("leaseTimeMs", String.valueOf(leaseTime)))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        verify(redisService).acquireLock(key, waitTime, leaseTime);
    }

    @Test
    void releaseLock_ShouldReleaseLock() throws Exception {
        // Given
        String key = "testLock";

        // When & Then
        mockMvc.perform(delete("/api/redis/lock/{key}", key))
                .andExpect(status().isOk());

        verify(redisService).releaseLock(key);
    }

    @Test
    void getValue_WhenKeyNotFound_ShouldReturnNull() throws Exception {
        // Given
        String key = "nonExistentKey";
        when(redisService.get(key)).thenReturn(null);

        // When & Then
        mockMvc.perform(get("/api/redis/{key}", key))
                .andExpect(status().isOk())
                .andExpect(content().string(""));

        verify(redisService).get(key);
    }

    @Test
    void setValue_WithInvalidTimeout_ShouldReturnBadRequest() throws Exception {
        // Given
        String key = "testKey";
        String value = "testValue";

        // When & Then
        mockMvc.perform(post("/api/redis/{key}", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(value))
                        .param("timeoutSeconds", "-1"))
                .andExpect(status().isBadRequest());

        verify(redisService, never()).set(anyString(), any(), anyLong());
    }
} 