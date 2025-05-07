package com.craftpilot.usermemoryservice.service;

import com.craftpilot.usermemoryservice.dto.UserInstructionRequest;
import com.craftpilot.usermemoryservice.model.UserInstruction;
import com.craftpilot.usermemoryservice.repository.UserInstructionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserInstructionService {
    private final UserInstructionRepository userInstructionRepository;

    public Mono<UserInstruction> createInstruction(String userId, UserInstructionRequest request) {
        log.info("Creating new instruction for userId: {}", userId);
        
        UserInstruction instruction = UserInstruction.builder()
                .userId(userId)
                .content(request.getContent())
                .priority(request.getPriority())
                .category(request.getCategory())
                .created(LocalDateTime.now())
                .lastUpdated(LocalDateTime.now())
                .build();
        
        return userInstructionRepository.save(instruction);
    }

    public Flux<UserInstruction> getUserInstructions(String userId) {
        log.info("Getting instructions for userId: {}", userId);
        return userInstructionRepository.findByUserId(userId);
    }

    public Mono<UserInstruction> getInstructionById(String instructionId) {
        log.info("Getting instruction by id: {}", instructionId);
        return userInstructionRepository.findById(instructionId);
    }

    public Mono<UserInstruction> updateInstruction(String instructionId, UserInstructionRequest request) {
        log.info("Updating instruction with id: {}", instructionId);
        
        return userInstructionRepository.findById(instructionId)
            .flatMap(existingInstruction -> {
                existingInstruction.setContent(request.getContent());
                existingInstruction.setPriority(request.getPriority());
                existingInstruction.setCategory(request.getCategory());
                existingInstruction.setLastUpdated(LocalDateTime.now());
                
                return userInstructionRepository.save(existingInstruction);
            });
    }

    public Mono<Void> deleteInstruction(String instructionId) {
        log.info("Deleting instruction with id: {}", instructionId);
        return userInstructionRepository.deleteById(instructionId);
    }

    public Mono<Void> deleteAllInstructions(String userId) {
        log.info("Deleting all instructions for userId: {}", userId);
        return userInstructionRepository.deleteByUserId(userId);
    }
}
