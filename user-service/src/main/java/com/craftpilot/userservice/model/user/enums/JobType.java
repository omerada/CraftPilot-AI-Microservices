package com.craftpilot.userservice.model.user.enums;

public enum JobType {
    // Teknoloji
    SOFTWARE_DEVELOPER,    // Yazılım Geliştirici
    SYSTEM_ADMIN,         // Sistem Yöneticisi
    DATA_SCIENTIST,       // Veri Bilimci
    DEVOPS_ENGINEER,      // DevOps Mühendisi
    UI_UX_DESIGNER,       // UI/UX Tasarımcı
    
    // İş ve Yönetim
    PRODUCT_MANAGER,      // Ürün Yöneticisi
    PROJECT_MANAGER,      // Proje Yöneticisi
    BUSINESS_ANALYST,     // İş Analisti
    MARKETING_SPECIALIST, // Pazarlama Uzmanı
    
    // Eğitim ve Araştırma
    TEACHER,              // Öğretmen
    RESEARCHER,           // Araştırmacı
    STUDENT,              // Öğrenci
    ACADEMIC,             // Akademisyen
    
    // Yaratıcı ve Medya
    CONTENT_CREATOR,      // İçerik Üretici
    GRAPHIC_DESIGNER,     // Grafik Tasarımcı
    DIGITAL_MARKETER,     // Dijital Pazarlamacı
    
    // Diğer
    FREELANCER,           // Serbest Çalışan
    ENTREPRENEUR,         // Girişimci
    OTHER;               // Diğer
    
    private String title;
    
    public String getTitle() {
        return this.name().toLowerCase().replace('_', ' ');
    }
}
