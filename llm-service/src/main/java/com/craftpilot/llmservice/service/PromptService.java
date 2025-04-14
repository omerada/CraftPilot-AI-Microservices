package com.craftpilot.llmservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PromptService {
    private final MeterRegistry meterRegistry;
    private final ObjectMapper objectMapper;
    
    private static final String PERFORMANCE_ANALYSIS_PROMPT_V1 = """
            Bu bir PageSpeed Insights JSON özetidir:
            {analysisData}
            
            Lütfen bu sonuçları analiz et ve tespit edilen performans sorunlarını önem sırasına göre listele.
            Her sorun için şu formatı kullan:
            
            1. Sorun: [sorunu kısaca açıkla]
            2. Önem Derecesi: [critical, major, minor] olarak belirt
            3. Çözüm: Frontend geliştiricisine yönelik net ve uygulanabilir adımlar ver
            4. Kod Örneği: Mümkünse çözüm için örnek kod parçası ekle
            5. Kaynaklar: Bu konuda daha fazla bilgi için kaynaklar öner
            
            Yanıtını JSON formatında ver. Başka bir şey yazma.
            """;
    
    public String getPerformanceAnalysisPrompt(Object analysisData) {
        try {
            String analysisJson = objectMapper.writeValueAsString(analysisData);
            String prompt = PERFORMANCE_ANALYSIS_PROMPT_V1.replace("{analysisData}", analysisJson);
            
            // Prompt kullanımını loğla ve metrik olarak kaydet
            log.debug("Performance analysis prompt generated with version: V1");
            meterRegistry.counter("prompt.performance_analysis.usage", "version", "v1").increment();
            
            return prompt;
        } catch (Exception e) {
            log.error("Error creating performance analysis prompt", e);
            throw new RuntimeException("Performans analizi promptu oluşturulamadı", e);
        }
    }
}
