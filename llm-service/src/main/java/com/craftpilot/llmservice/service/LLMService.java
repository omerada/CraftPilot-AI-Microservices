@Service
@Slf4j
@RequiredArgsConstructor
public class LLMService {

    public Mono<AIResponse> processChatCompletion(AIRequest request) {
        long startTime = System.currentTimeMillis();
        
        return processRequest(request)
            .map(result -> AIResponse.builder()
                .requestId(request.getRequestId())
                .response(result)
                .model(request.getModel())
                .tokensUsed(calculateTokens(result))
                .processingTime(System.currentTimeMillis() - startTime)
                .status("SUCCESS")
                .build())
            .doOnSuccess(response -> log.debug("Generated response: {}", response))
            .doOnError(error -> log.error("Error processing request: ", error));
    }

    // ... diğer metodlar
}
