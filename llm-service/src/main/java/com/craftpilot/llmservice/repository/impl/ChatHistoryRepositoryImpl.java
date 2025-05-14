package com.craftpilot.llmservice.repository.impl;

import com.craftpilot.llmservice.model.ChatHistory;
import com.craftpilot.llmservice.model.Conversation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Slf4j
@RequiredArgsConstructor
public class ChatHistoryRepositoryImpl {

        private final ReactiveMongoTemplate mongoTemplate;

        public Mono<ChatHistory> addConversation(String historyId, Conversation conversation) {
                log.debug("Adding conversation to history with ID: {}", historyId);
                Query query = new Query(Criteria.where("id").is(historyId));
                Update update = new Update()
                                .push("conversations", conversation)
                                .set("updatedAt", LocalDateTime.now())
                                .set("lastConversation", conversation.getContent()); // Changed from getMessage() to
                                                                                     // getContent()

                return mongoTemplate.findAndModify(query, update, ChatHistory.class)
                                .doOnNext(result -> log.debug("Added conversation to history: {}", historyId))
                                .doOnError(e -> log.error("Error adding conversation to history {}: {}", historyId,
                                                e.getMessage()));
        }

        public Mono<ChatHistory> updateTitle(String historyId, String newTitle) {
                log.debug("Updating title for history with ID: {} to '{}'", historyId, newTitle);
                Query query = new Query(Criteria.where("id").is(historyId));
                Update update = new Update()
                                .set("title", newTitle)
                                .set("updatedAt", LocalDateTime.now());

                return mongoTemplate.findAndModify(query, update, ChatHistory.class)
                                .doOnNext(result -> log.debug("Updated title for history: {}", historyId))
                                .doOnError(e -> log.error("Error updating title for history {}: {}", historyId,
                                                e.getMessage()));
        }

        public Mono<Void> delete(String id) {
                log.debug("Deleting chat history with ID: {}", id);
                Query query = new Query(Criteria.where("id").is(id));
                return mongoTemplate.remove(query, ChatHistory.class)
                                .then()
                                .doOnSuccess(v -> log.debug("Successfully deleted chat history: {}", id))
                                .doOnError(e -> log.error("Error deleting chat history {}: {}", id, e.getMessage()));
        }
}
