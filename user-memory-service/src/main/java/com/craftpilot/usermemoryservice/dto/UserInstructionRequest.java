package com.craftpilot.usermemoryservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInstructionRequest {
    
    @NotBlank(message = "Talimat içeriği boş olamaz")
    @Size(min = 2, max = 1000, message = "Talimat içeriği 2-1000 karakter arasında olmalıdır")
    private String content;
    
    @Builder.Default
    private Integer priority = 1;
    
    private String category;
}
