package com.medsyncpro.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class EmailService {
    
    @Value("${brevo.api.key}")
    private String brevoApiKey;
    
    @Value("${app.frontend.url}")
    private String frontendUrl;
    
    @Value("${brevo.from.email}")
    private String fromEmail;
    
    @Value("${brevo.from.name}")
    private String fromName;
    
    @Async
    public void sendVerificationEmail(String email, String token) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("api-key", brevoApiKey);
            
            Map<String, Object> body = Map.of(
                "sender", Map.of("name", fromName, "email", fromEmail),
                "to", List.of(Map.of("email", email)),
                "subject", "Verify Your Email - MedSyncPro",
                "htmlContent", "<p>Click the link to verify your email:</p>" +
                            "<p><a href='" + frontendUrl + "verify-email?token=" + token + "'>Verify Email</a></p>" +
                            "<p>This link will expire in 24 hours.</p>"
            );
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            restTemplate.postForEntity("https://api.brevo.com/v3/smtp/email", request, String.class);
            
            log.info("Verification email sent to: {}", email);
        } catch (Exception e) {
            log.error("Failed to send verification email to: {}", email, e);
        }
    }
}
