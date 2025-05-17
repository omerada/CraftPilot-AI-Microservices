package com.craftpilot.userservice.controller;

import com.craftpilot.userservice.service.ModelDataLoader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/models")
@RequiredArgsConstructor
@Slf4j
public class ModelLoaderController {

    private final ModelDataLoader modelDataLoader;

    @PostMapping("/reload")
    public Mono<ResponseEntity<Map<String, Object>>> reloadModels() {
        log.info("Model verilerini yeniden yükleme API çağrısı alındı");
        
        return modelDataLoader.reloadModelData()
            .map(count -> {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "AI model verileri başarıyla yüklendi");
                response.put("loadedModels", count);
                return ResponseEntity.ok(response);
            })
            .onErrorResume(e -> {
                log.error("Model yeniden yüklenirken hata: {}", e.getMessage(), e);
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Model yükleme hatası: " + e.getMessage());
                return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response));
            });
    }
    
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getModelLoaderStatus() {
        log.info("Model yükleme durumu API çağrısı alındı");
        return ResponseEntity.ok(modelDataLoader.getModelLoadingStatus());
    }
    
    @PostMapping("/load-from-file")
    public Mono<ResponseEntity<Map<String, Object>>> loadFromCustomFile(@RequestBody Map<String, String> request) {
        String filePath = request.get("filePath");
        if (filePath == null || filePath.isEmpty()) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Dosya yolu belirtilmedi");
            return Mono.just(ResponseEntity.badRequest().body(response));
        }
        
        log.info("Özel dosyadan model yükleme başlatılıyor: {}", filePath);
        
        // Dosya yolu güvenlik kontrolü
        if (!filePath.startsWith("classpath:") && !filePath.startsWith("file:") && !filePath.startsWith("http")) {
            filePath = "file:" + filePath;
        }
        
        final String validatedFilePath = filePath;
        
        return modelDataLoader.loadModelsFromFile(validatedFilePath)
            .map(count -> {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "Özel dosyadan AI model verileri başarıyla yüklendi");
                response.put("loadedModels", count);
                response.put("filePath", validatedFilePath);
                response.put("timestamp", System.currentTimeMillis());
                return ResponseEntity.ok(response);
            })
            .onErrorResume(e -> {
                log.error("Özel dosyadan model yüklenirken hata: {}", e.getMessage(), e);
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Özel dosyadan model yükleme hatası: " + e.getMessage());
                response.put("filePath", validatedFilePath);
                response.put("errorType", e.getClass().getSimpleName());
                response.put("timestamp", System.currentTimeMillis());
                return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response));
            });
    }
}
