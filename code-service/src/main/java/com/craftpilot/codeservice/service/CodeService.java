package com.craftpilot.codeservice.service;

import com.craftpilot.codeservice.model.Code;
import com.craftpilot.codeservice.repository.CodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class CodeService {
    private final CodeRepository codeRepository;
    private final OpenAIService openAIService;

    public Mono<Code> generateCode(Code code) {
        return openAIService.generateCode(code.getPrompt(), code.getLanguage(), code.getFramework())
                .map(generatedCode -> {
                    code.setGeneratedCode(generatedCode);
                    return code;
                })
                .flatMap(codeRepository::save);
    }

    public Mono<Code> getCode(String id) {
        return codeRepository.findById(id);
    }

    public Flux<Code> getUserCodes(String userId) {
        return codeRepository.findByUserId(userId);
    }

    public Mono<Void> deleteCode(String id) {
        return codeRepository.deleteById(id);
    }
} 