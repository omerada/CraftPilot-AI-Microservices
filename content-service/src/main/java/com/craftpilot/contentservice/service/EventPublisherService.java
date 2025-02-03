package com.craftpilot.contentservice.service;

import com.craftpilot.contentservice.model.Content;

public interface EventPublisherService {
    void publishContentCreated(Content content);
    void publishContentUpdated(Content content);
    void publishContentDeleted(String id, String userId);
    void publishContentPublished(Content content);
    void publishContentArchived(Content content);
} 