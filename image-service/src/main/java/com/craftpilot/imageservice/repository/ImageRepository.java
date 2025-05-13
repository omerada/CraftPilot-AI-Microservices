package com.craftpilot.imageservice.repository;

import com.craftpilot.imageservice.model.Image;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface ImageRepository extends ReactiveMongoRepository<Image, String> {
    
    Flux<Image> findByUserId(String userId);
}