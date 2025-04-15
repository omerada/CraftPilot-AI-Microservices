package com.craftpilot.lighthouseservice.util;

import org.springframework.stereotype.Component;

import java.net.MalformedURLException;
import java.net.URL;

@Component
public class UrlValidator {
    
    public void validate(String urlString) {
        if (urlString == null || urlString.isEmpty()) {
            throw new IllegalArgumentException("URL cannot be empty");
        }
        
        try {
            URL url = new URL(urlString);
            String protocol = url.getProtocol();
            if (!protocol.equals("http") && !protocol.equals("https")) {
                throw new IllegalArgumentException("URL must use http or https protocol");
            }
            
            if (url.getHost().isEmpty()) {
                throw new IllegalArgumentException("URL must have a valid host");
            }
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Invalid URL format: " + e.getMessage());
        }
    }
}
