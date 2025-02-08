package com.craftpilot.notificationservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class EmailRequest {
    
    @NotBlank(message = "Alıcı e-posta adresi gereklidir")
    @Email(message = "Geçerli bir e-posta adresi giriniz")
    private String to;
    
    @NotBlank(message = "Konu gereklidir")
    private String subject;
    
    @NotBlank(message = "İçerik gereklidir")
    private String body;
} 