package com.craftpilot.creditservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "credits")
public class Credit {
    @Id
    private String id;
    
    @Indexed(unique = true)
    private String userId;
    
    private double balance;
    private double lifetimeEarned;
    private double lifetimeSpent;
    
    private boolean deleted;
    
    private Date lastUpdated;
    private Date createdAt;
}