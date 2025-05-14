package com.craftpilot.usermemoryservice.repository;

import com.craftpilot.usermemoryservice.model.UserInstruction;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface UserInstructionRepository extends ReactiveMongoRepository<UserInstruction, String> {
    
    Flux<UserInstruction> findByUserId(String userId);
    
    Mono<Void> deleteByUserId(String userId);
}
