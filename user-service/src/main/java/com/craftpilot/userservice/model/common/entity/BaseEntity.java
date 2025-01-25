package com.craftpilot.userservice.model.common.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import java.time.LocalDateTime;

/**
 * Base entity class with common fields for audit tracking and lifecycle management for Firestore.
 * Provides manual population of audit fields.
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class BaseEntity {

    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime updatedAt;
    private String updatedBy;

    /**
     * Manually sets the createdBy and createdAt fields when the entity is created.
     * @param uid the identifier of the user creating the entity
     */
    public void setCreatedInfo(String uid) {
        this.createdBy = uid;
        this.createdAt = LocalDateTime.now();
    }

    /**
     * Manually sets the updatedBy and updatedAt fields when the entity is updated.
     * @param uid the identifier of the user updating the entity
     */
    public void setUpdatedInfo(String uid) {
        this.updatedBy = uid;
        this.updatedAt = LocalDateTime.now();
    }

}
