package com.craftpilot.llmservice.service;

import com.craftpilot.llmservice.model.AIRequest;
import com.craftpilot.llmservice.model.AIResponse;
import com.craftpilot.llmservice.model.StreamResponse;  // Yeni import
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.time.Duration;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.reactive.function.client.ClientResponse;
import com.craftpilot.llmservice.exception.APIException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Objects;

@Service
@Slf4j
@RequiredArgsConstructor
public class LLMService {
    private final WebClient openRouterWebClient; 
    private final ObjectMapper objectMapper;
    
    // Model başına maksimum token limitleri
    private static final Map<String, Integer> MODEL_TOKEN_LIMITS = Map.ofEntries(
        Map.entry("google/gemini-2.0-flash-lite-preview-02-05:free", 30000),
        Map.entry("google/gemini-pro", 30000),
        Map.entry("google/palm-2-codechat-bison", 8000),
        Map.entry("google/palm-2-chat-bison", 8000),
        Map.entry("anthropic/claude-3-haiku", 48000),
        Map.entry("anthropic/claude-3-sonnet", 180000),
        Map.entry("anthropic/claude-3-opus", 180000),
        Map.entry("anthropic/claude-2", 100000),
        Map.entry("openai/gpt-4", 8000),
        Map.entry("openai/gpt-4-turbo", 128000),
        Map.entry("openai/gpt-3.5-turbo", 16000),
        Map.entry("meta-llama/llama-3-70b-instruct", 8000),
        Map.entry("meta-llama/llama-3-8b-instruct", 8000),
        Map.entry("mistral/mistral-large", 32000),
        Map.entry("mistral/mistral-medium", 32000),
        Map.entry("mistral/mistral-small", 32000)
    );
    // Varsayılan token limiti
    private static final int DEFAULT_TOKEN_LIMIT = 4000;

    public Mono<AIResponse> processChatCompletion(AIRequest request) {
        return callOpenRouter("/chat/completions", request)
            .doOnNext(response -> log.debug("OpenRouter yanıtı: {}", response))
            .map(response -> {
                try {
                    return mapToAIResponse(response, request);
                } catch (Exception e) {
                    log.error("AI yanıtı işlenirken hata: {}", e.getMessage(), e);
                    throw new RuntimeException("AI yanıtı haritalanırken hata: " + e.getMessage(), e);
                }
            })
            .timeout(Duration.ofSeconds(60))
            .doOnError(e -> log.error("Chat completion error: {}", e.getMessage(), e))
            .onErrorResume(e -> {
                log.error("Hata yakalandı ve işlendi: {}", e.getMessage());
                return Mono.just(AIResponse.error("AI servisi hatası: " + e.getMessage()));
            });
    }

    public Mono<AIResponse> processCodeCompletion(AIRequest request) {
        // Code completion için özel model seçimi
        // request.setModel("anthropic/claude-2");
        return callOpenRouter("/v1/chat/completions", request)
            .map(response -> mapToAIResponse(response, request));
    }

    // OpenRouter görsel modeli desteklemediği için alternatif servis kullanımı
    public Mono<AIResponse> processImageGeneration(AIRequest request) {
        return Mono.error(new UnsupportedOperationException(
            "Image generation is not supported by OpenRouter. Please use Stability AI or DALL-E API directly."));
    }

    public Flux<StreamResponse> streamChatCompletion(AIRequest request) {
        Map<String, Object> requestBody = createRequestBody(request);
        requestBody.put("stream", true);
        
        log.info("Starting streaming request with model: {}", request.getModel());
        
        return openRouterWebClient.post()
            .uri("/chat/completions")  
            .bodyValue(requestBody)
            .headers(headers -> {
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("Accept", "text/event-stream");
                headers.set("Connection", "keep-alive");
                headers.set("Cache-Control", "no-cache");
            })
            .retrieve()
            .bodyToFlux(String.class)
            .doOnSubscribe(s -> log.info("⚡ Stream connection established"))
            // Hiçbir buffer olmadan her yanıtı hemen işle
            .publishOn(Schedulers.immediate())
            // Debugging için tüm ham yanıtları logla
            .doOnNext(chunk -> log.debug("Raw chunk received: {}", chunk))
            .filter(chunk -> chunk != null && !chunk.trim().isEmpty())
            .mapNotNull(chunk -> {  // mapNotNull kullanarak null değerleri filtrele
                try {
                    if (chunk.startsWith("data: ")) {
                        chunk = chunk.substring(6).trim();
                    }
                    if (chunk.equals("[DONE]")) {
                        log.debug("Stream completion signal received");
                        return StreamResponse.builder()
                            .content("")
                            .done(true)
                            .build();
                    }
                    
                    Map<String, Object> response = objectMapper.readValue(chunk, new TypeReference<Map<String, Object>>() {});
                    
                    if (response.containsKey("choices")) {
                        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
                        if (!choices.isEmpty()) {
                            Map<String, Object> choice = choices.get(0);
                            Map<String, Object> delta = (Map<String, Object>) choice.get("delta");
                            
                            String content = "";
                            if (delta != null && delta.containsKey("content")) {
                                content = (String) delta.get("content");
                                if (content != null && !content.isEmpty()) {
                                    log.debug("Streaming content: {}", content);
                                    return StreamResponse.builder()
                                        .content(content)
                                        .done(false)
                                        .build();
                                }
                            }
                            
                            boolean isDone = choice.containsKey("finish_reason") && 
                                           choice.get("finish_reason") != null;
                            if (isDone) {
                                return StreamResponse.builder()
                                    .content("")
                                    .done(true)
                                    .build();
                            }
                        }
                    }
                    
                    // Eğer buraya kadar geldiyse ve içerik yoksa, bu chunk'ı atla
                    log.debug("Skipping empty or invalid chunk: {}", chunk);
                    return null;
                    
                } catch (Exception e) {
                    log.error("Error processing chunk: {} - Error: {}", chunk, e.getMessage());
                    return StreamResponse.builder()
                        .content("Error: " + e.getMessage())
                        .done(true)
                        .build();
                }
            })
            .filter(Objects::nonNull)  // Ekstra güvenlik için null kontrolü
            // Oluşması halinde hata durumunu istemciye bildir
            .onErrorResume(e -> {
                log.error("Stream processing error: {}", e.getMessage(), e);
                return Flux.just(StreamResponse.builder()
                    .content("Error: " + e.getMessage())
                    .done(true)
                    .build());
            });
    }

    public Mono<AIResponse> enhancePrompt(AIRequest request) {
        // Varsayılan değerler için sıcaklık ve maksimum token değerlerini ayarla
        if (request.getTemperature() == null) {
            request.setTemperature(0.3); // Daha kararlı sonuçlar için düşük sıcaklık
        }
        
        // Token limitini ayarla
        if (request.getMaxTokens() == null) {
            request.setMaxTokens(32000); // Prompt iyileştirme için daha küçük token limiti yeterli
        }
        
        // Sistem promptunu hazırla
        String systemPrompt = getPromptEnhancementSystemPrompt(request.getLanguage());
        
        // Mesajları oluştur
        List<Map<String, Object>> messages = new ArrayList<>();
        
        // Sistem mesajını ekle
        Map<String, Object> systemMessage = new HashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content", systemPrompt);
        messages.add(systemMessage);
        
        // Kullanıcı mesajını ekle
        Map<String, Object> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", request.getPrompt());
        messages.add(userMessage);
        
        // İstek özelliklerini ayarla
        request.setMessages(messages);
        request.setModel("google/gemma-3-27b-it:free"); 
        
        // Chat completion API'sini kullanarak istek gönder
        return callOpenRouter("/chat/completions", request)
            .map(response -> {
                // Sadece enhance-prompt için model ve token bilgilerini çıkararak özel yanıt oluştur
                String responseText = extractResponseText(response);
                return AIResponse.builder()
                    .response(responseText)
                    .requestId(request.getRequestId())
                    .success(true)
                    .build();
            })
            .timeout(Duration.ofSeconds(30))
            .doOnError(e -> log.error("Prompt iyileştirme hatası: {}", e.getMessage(), e))
            .onErrorResume(e -> {
                log.error("Prompt iyileştirme hatası yakalandı: {}", e.getMessage());
                return Mono.just(AIResponse.error("Prompt iyileştirilemedi: " + e.getMessage()));
            });
    }

    /**
     * Prompt geliştirme için özel sistem promptunu döndürür
     */
    private String getPromptEnhancementSystemPrompt(String language) {
        // İngilizce sistem promptu
        String englishSystemPrompt = "You are an expert prompt engineer. Your task is to transform the user's text into a more effective prompt that will be directed to an AI chat model.\n\n" +
            "Follow these steps:\n" +
            "1. Create a clearer and more detailed expression\n" +
            "2. Clarify context and objectives\n" +
            "3. Remove unnecessary words\n" +
            "4. Use more specific and descriptive language\n" +
            "5. Emphasize important details\n" +
            "6. Structure and organize the prompt well\n" +
            "7. Specify the response format if necessary\n\n" +
            "Important rules:\n" +
            "- Preserve the user's original language\n" +
            "- Only create a better prompt, don't do anything else\n" +
            "- If small changes are sufficient, don't modify the original too much\n" +
            "- Don't add additional explanations, ONLY return the improved prompt text\n\n" +
            "User text to improve:\n" +
            "\"{prompt}\"";
        
        // Türkçe sistem promptu
        String turkishSystemPrompt = "Sen uzman bir prompt mühendisisin. Kullanıcının verdiği metni bir AI sohbet modeline yöneltilecek daha etkili bir prompt'a dönüştürmekle görevlisin.\n\n" +
            "Aşağıdaki adımları izle:\n" +
            "1. Daha net ve detaylı bir ifade oluştur\n" +
            "2. Bağlam ve hedefleri netleştir\n" +
            "3. Gereksiz kelimelerden arındır\n" +
            "4. Daha spesifik ve açıklayıcı bir dil kullan\n" +
            "5. Önemli detayları vurgula\n" +
            "6. Yapılandırılmış ve iyi organize edilmiş bir prompt format\n" +
            "7. Gerektiğinde yanıt formatını belirt\n\n" +
            "Önemli kurallar:\n" +
            "- Kullanıcının orijinal dilini koru\n" +
            "- Sadece daha iyi bir prompt oluştur, farklı bir şey yapma\n" +
            "- Küçük değişiklikler yeterli ise orijinali fazla değiştirme\n" +
            "- Ek açıklamalar ekleme, SADECE iyileştirilmiş prompt metnini döndür";
        
        // Dil tercihine göre uygun sistem promptunu döndür
        if (language != null && language.equalsIgnoreCase("tr")) {
            return turkishSystemPrompt;
        }
        
        return englishSystemPrompt;
    }

    private Mono<Map<String, Object>> callOpenRouter(String endpoint, AIRequest request) {
        Map<String, Object> requestBody = createRequestBody(request);
        log.debug("OpenRouter isteği: {} - Body: {}", endpoint, requestBody);
        
        return openRouterWebClient.post()
            .uri(endpoint)
            .bodyValue(requestBody)
            .headers(headers -> {
                headers.setContentType(MediaType.APPLICATION_JSON);
                // Birden fazla kabul edilebilir içerik türü belirtiyoruz
                headers.set("Accept", "application/json, text/plain, text/html, */*");
            })
            .exchangeToMono(response -> {
                if (response.statusCode().is2xxSuccessful()) {
                    // İçerik türünü kontrol et
                    MediaType contentType = response.headers().contentType().orElse(MediaType.APPLICATION_JSON);
                    
                    if (contentType.includes(MediaType.APPLICATION_JSON)) {
                        // JSON yanıtı - normal işleme
                        return response.bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {});
                    } else if (contentType.includes(MediaType.TEXT_HTML) || 
                               contentType.includes(MediaType.TEXT_PLAIN)) {
                        // HTML veya düz metin yanıtı - metni al ve bir hata mesajı oluştur
                        return response.bodyToMono(String.class)
                                .flatMap(htmlContent -> {
                                    log.error("HTML yanıtı alındı: {} karakterlik içerik", 
                                        htmlContent != null ? htmlContent.length() : 0);
                                    Map<String, Object> errorMap = new HashMap<>();
                                    errorMap.put("error", "API HTML yanıtı döndü. Servis geçici olarak kullanılamıyor olabilir.");
                                    return Mono.just(errorMap);
                                });
                    } else {
                        // Diğer içerik türleri - bilgi veren bir hata yanıtı döndür
                        log.warn("Beklenmeyen içerik türü: {}", contentType);
                        Map<String, Object> errorMap = new HashMap<>();
                        errorMap.put("error", "Beklenmeyen içerik türü: " + contentType);
                        return Mono.just(errorMap);
                    }
                } else {
                    // Hata durumları için
                    return response.bodyToMono(String.class)
                        .flatMap(error -> {
                            String errorMessage = "API hatası: " + response.statusCode() + 
                                " - Yanıt: " + (error != null ? error : "Boş yanıt");
                            log.error(errorMessage);
                            Map<String, Object> errorMap = new HashMap<>();
                            errorMap.put("error", errorMessage);
                            return Mono.just(errorMap);
                        })
                        .onErrorResume(e -> {
                            log.error("API yanıtı okunurken hata: {}", e.getMessage());
                            Map<String, Object> errorMap = new HashMap<>();
                            errorMap.put("error", "API yanıtı işlenirken hata: " + e.getMessage());
                            return Mono.just(errorMap);
                        });
                }
            })
            .timeout(Duration.ofSeconds(30))
            .doOnError(e -> log.error("OpenRouter API isteği sırasında hata: {}", e.getMessage(), e));
    }

    private Mono<? extends Throwable> handleError(ClientResponse response) {
        return response.bodyToMono(String.class)
            .flatMap(error -> {
                String message = String.format("OpenRouter API Error [%s]: %s", 
                    response.statusCode(), error);
                log.error(message);
                return Mono.error(new APIException(message));
            });
    }

    private Map<String, Object> createRequestBody(AIRequest request) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", request.getModel());
        
        List<Map<String, Object>> messages;
        
        // Eğer messages dizisi mevcutsa, onu kullan
        if (request.getMessages() != null && !request.getMessages().isEmpty()) {
            messages = new ArrayList<>(request.getMessages());
            
            // Sistem mesajı var mı kontrol et
            boolean hasSystemMessage = messages.stream()
                .anyMatch(msg -> "system".equals(msg.get("role")));
            
            // Yoksa ekle
            if (!hasSystemMessage) {
                Map<String, Object> systemMessage = new HashMap<>();
                systemMessage.put("role", "system");
                systemMessage.put("content", getSystemPrompt(request.getRequestType(), request.getLanguage()));
                
                // Sistem mesajını listenin başına ekle
                messages.add(0, systemMessage);
            }
        } 
        // Değilse, prompt alanından messages oluştur (geriye dönük uyumluluk)
        else if (request.getPrompt() != null && !request.getPrompt().isEmpty()) {
            messages = new ArrayList<>();
            
            // Sistem mesajını ekle
            Map<String, Object> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", getSystemPrompt(request.getRequestType(), request.getLanguage()));
            messages.add(systemMessage);
            
            // Kullanıcı mesajını ekle
            Map<String, Object> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", request.getPrompt());
            messages.add(userMessage);
        } else {
            // Her iki alan da boşsa, hata fırlat
            throw new IllegalArgumentException("Request must contain either 'prompt' or 'messages'");
        }
        
        body.put("messages", messages);
        body.put("max_tokens", request.getMaxTokens());
        body.put("temperature", request.getTemperature());
        
        log.debug("Oluşturulan request body: {}", body);
        return body;
    }

    /**
     * Request tipine ve dile göre özel sistem prompt'u oluşturur
     */
    private String getSystemPrompt(String requestType, String language) {
        String basePrompt;
        
        if ("CODE".equalsIgnoreCase(requestType)) {
            basePrompt = "You are an expert coding assistant. Provide clean, efficient, and well-documented code solutions. " +
                   "Explain your approach briefly when helpful, focusing on best practices and performance considerations. " +
                   "When providing code, ensure it's production-ready and includes appropriate error handling.";
        } else if ("CHAT".equalsIgnoreCase(requestType)) {
            basePrompt = "You are a helpful, accurate, and thoughtful assistant. Provide clear, concise, and relevant responses. " +
                   "Maintain context throughout the conversation and ask clarifying questions when necessary. " +
                   "Balance thoroughness with brevity based on the user's needs. " +
                   "Always aim to provide factually correct information and acknowledge limitations in your knowledge.";
        } else {
            basePrompt = "You are a helpful AI assistant. Provide accurate, relevant, and detailed responses to the user's requests.";
        }
        
        // Kullanıcının diline göre yanıt vermesi için ek talimatlar ekle
        if (language != null && !language.equalsIgnoreCase("en")) {
            String languageName = getLanguageName(language);
            return basePrompt + " Always respond in " + languageName + " language unless explicitly asked to use a different language.";
        }
        
        return basePrompt;
    }

    /**
     * Dil kodundan dil ismini döndürür
     */
    private String getLanguageName(String languageCode) {
        Map<String, String> languageMap = buildSystemPrompts();
        
        return languageMap.getOrDefault(languageCode.toLowerCase(), "the user's preferred language (" + languageCode + ")");
    }

    private Map<String, String> buildSystemPrompts() {
        Map<String, String> prompts = new HashMap<>();
        
        // Ana promptları ekle
        prompts.put("DEFAULT", "You are a helpful AI assistant.");
        prompts.put("DEFAULT_TR", "Yardımsever bir yapay zeka asistanısın.");
        prompts.put("CODE", "You are an expert software developer. Write clean, efficient, and well-documented code.");
        prompts.put("CODE_TR", "Uzman bir yazılım geliştiricisisin. Temiz, verimli ve iyi dokümante edilmiş kod yaz.");
        prompts.put("CHAT", "You are a friendly conversational AI. Be helpful and engaging.");
        prompts.put("CHAT_TR", "Arkadaş canlısı bir sohbet yapay zekasısın. Yardımsever ve ilgi çekici ol.");
        prompts.put("EXPLAIN", "You are a skilled teacher. Explain concepts clearly and thoroughly.");
        prompts.put("EXPLAIN_TR", "Yetenekli bir öğretmensin. Kavramları net ve detaylı açıkla.");
        prompts.put("ANALYZE", "You are an analytical expert. Provide detailed analysis and insights.");
        prompts.put("ANALYZE_TR", "Analitik bir uzmanısın. Detaylı analiz ve içgörüler sun.");
        prompts.put("BRAINSTORM", "You are a creative thinker. Generate innovative ideas and solutions.");
        prompts.put("BRAINSTORM_TR", "Yaratıcı bir düşünürsün. Yenilikçi fikirler ve çözümler üret.");
        prompts.put("DATA", "You are a data analyst. Process and explain data patterns effectively.");
        prompts.put("DATA_TR", "Bir veri analistisin. Veri desenlerini etkili şekilde işle ve açıkla.");
        prompts.put("REVIEW", "You are a thorough reviewer. Provide constructive and detailed feedback.");
        prompts.put("REVIEW_TR", "Detaylı bir eleştirmensin. Yapıcı ve ayrıntılı geri bildirim ver.");
        prompts.put("PLAN", "You are a strategic planner. Create organized and actionable plans.");
        prompts.put("PLAN_TR", "Stratejik bir planlayıcısın. Organize ve uygulanabilir planlar oluştur.");
        prompts.put("SUMMARIZE", "You are an efficient summarizer. Extract and convey key information concisely.");
        prompts.put("SUMMARIZE_TR", "Verimli bir özetleyicisin. Önemli bilgileri özlü bir şekilde çıkar ve ilet.");
        prompts.put("DEBUG", "You are a skilled debugger. Help identify and fix technical issues.");
        prompts.put("DEBUG_TR", "Yetenekli bir hata ayıklayıcısın. Teknik sorunları belirleme ve çözme konusunda yardımcı ol.");
        
        return Map.copyOf(prompts);  // Değiştirilemez kopya dön
    }

    private AIResponse mapToAIResponse(Map<String, Object> openRouterResponse, AIRequest request) {
        log.debug("OpenRouter yanıtı haritalanıyor: {}", openRouterResponse);
        
        String responseText = extractResponseText(openRouterResponse);
        if (responseText == null || responseText.trim().isEmpty()) {
            log.warn("OpenRouter'dan boş yanıt alındı");
            throw new RuntimeException("AI servisinden boş yanıt alındı");
        }

        AIResponse response = AIResponse.success(
            responseText,
            request.getModel(),
            extractTokenCount(openRouterResponse),
            request.getRequestId()
        );

        log.debug("Haritalanan yanıt: {}", response);
        return response;
    }

    private String extractResponseText(Map<String, Object> response) {
        try {
            log.debug("Yanıt içeriği: {}", response);
            
            // API hata mesajlarını kontrol et
            if (response.containsKey("error")) {
                Object errorObj = response.get("error");
                if (errorObj instanceof String) {
                    throw new RuntimeException("API hatası: " + errorObj);
                } else if (errorObj instanceof Map) {
                    Map<String, Object> errorMap = (Map<String, Object>) errorObj;
                    String errorMessage = errorMap.containsKey("message") 
                        ? errorMap.get("message").toString() 
                        : "Bilinmeyen API hatası";
                    throw new RuntimeException("API hatası: " + errorMessage);
                }
                throw new RuntimeException("API hatası: " + errorObj);
            }

            // Daha ayrıntılı debugging ekleyelim
            log.debug("Response keys: {}", response.keySet());
            
            if (response.containsKey("choices")) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
                log.debug("Choices size: {}", choices.size());
                
                if (!choices.isEmpty()) {
                    Map<String, Object> choice = choices.get(0);
                    log.debug("Choice keys: {}", choice.keySet());
                    
                    // Choice içinde message veya text olabilir
                    if (choice.containsKey("message")) {
                        Map<String, Object> message = (Map<String, Object>) choice.get("message");
                        log.debug("Message keys: {}", message.keySet());
                        
                        if (message.containsKey("content")) {
                            Object content = message.get("content");
                            log.debug("Content type: {}", content != null ? content.getClass().getName() : "null");
                            
                            if (content instanceof String) {
                                return (String) content;
                            } else if (content instanceof List) {
                                List<Map<String, Object>> contents = (List<Map<String, Object>>) content;
                                return contents.stream()
                                    .filter(item -> "text".equals(item.get("type")))
                                    .map(item -> (String) item.get("text"))
                                    .findFirst()
                                    .orElse("");
                            }
                        }
                    }
                    // GPT türü modeller için text alanını kontrol et
                    else if (choice.containsKey("text")) {
                        return (String) choice.get("text");
                    }
                    // Ayrıca delta içinde de olabilir (özellikle stream yanıtlarında)
                    else if (choice.containsKey("delta")) {
                        Map<String, Object> delta = (Map<String, Object>) choice.get("delta");
                        if (delta.containsKey("content")) {
                            return (String) delta.get("content");
                        }
                    }
                }
            }
            
            // Farklı yanıt formatlarını işle
            if (response.containsKey("output") && response.get("output") instanceof String) {
                return (String) response.get("output");
            }
            
            if (response.containsKey("completion") && response.get("completion") instanceof String) {
                return (String) response.get("completion");
            }
            
            if (response.containsKey("generated_text") && response.get("generated_text") instanceof String) {
                return (String) response.get("generated_text");
            }
            
            // Eğer response'un kendisi String ise direkt döndür (bazı LLM API'leri için)
            if (response.size() == 1 && response.values().iterator().next() instanceof String) {
                return (String) response.values().iterator().next();
            }
            
            // Hata durumunda anlamlı bir hata mesajı oluştur
            StringBuilder detailBuilder = new StringBuilder("API yanıt anahtarları: ");
            for (String key : response.keySet()) {
                Object value = response.get(key);
                detailBuilder.append(key).append("=");
                if (value == null) {
                    detailBuilder.append("null");
                } else if (value instanceof String) {
                    String strVal = (String)value;
                    detailBuilder.append(strVal.length() > 50 ? strVal.substring(0, 50) + "..." : strVal);
                } else {
                    detailBuilder.append(value.getClass().getSimpleName());
                }
                detailBuilder.append(", ");
            }
            
            // Yanıt formatını JSON olarak logla
            try {
                log.error("Bilinmeyen yanıt formatı: {}", new ObjectMapper().writeValueAsString(response));
            } catch (Exception e) {
                log.error("Yanıt JSON dönüştürme hatası", e);
            }
            
            throw new RuntimeException("Geçersiz API yanıt formatı: " + detailBuilder.toString());
        } catch (Exception e) {
            log.error("Yanıt işlenirken hata oluştu: {}", e.getMessage(), e);
            throw new RuntimeException("AI yanıtı işlenemedi: " + e.getMessage(), e);
        }
    }

    private Integer extractTokenCount(Map<String, Object> response) {
        Map<String, Object> usage = (Map<String, Object>) response.getOrDefault("usage", Map.of());
        return ((Number) usage.getOrDefault("total_tokens", 0)).intValue();
    }
}