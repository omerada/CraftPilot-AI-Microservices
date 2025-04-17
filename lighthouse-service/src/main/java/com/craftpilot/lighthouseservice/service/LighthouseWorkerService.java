package com.craftpilot.lighthouseservice.service;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class LighthouseWorkerService {
    
    @Value("${lighthouse.worker.count:2}")
    private int configuredWorkerCount;
    
    /**
     * Aktif worker sayısını döndürür.
     * Gerçek bir worker yönetimi uygulaması için, Redis'ten gerçek worker sayısını alabilirsiniz.
     * 
     * @return Aktif worker sayısı
     */
    public int getActiveWorkerCount() {
        // Bu basit implementasyon yapılandırılmış worker sayısını döndürür
        // Gerçek uygulamada, Redis'e veya başka bir durum depolama mekanizmasına bağlanarak
        // aktif worker sayısını öğrenmek daha iyi olur
        return configuredWorkerCount;
    }
}
