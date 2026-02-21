package com.medsyncpro.service;

import com.medsyncpro.entity.Role;
import com.medsyncpro.entity.User;
import com.medsyncpro.entity.VerificationToken;
import com.medsyncpro.exception.BusinessException;
import com.medsyncpro.repository.UserRepository;
import com.medsyncpro.repository.VerificationTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VerificationService {
    private final VerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    
    @Value("${verification.token.expiry.hours:24}")
    private int tokenExpiryHours;
    
    @Value("${verification.resend.limit:5}")
    private int resendLimit;
    
    @Transactional
    public void generateAndSendToken(User user) {
        // Check resend limit
        long recentCount = tokenRepository.countRecentTokensByEmail(
            user.getEmail(), 
            LocalDateTime.now().minusHours(1)
        );
        if (recentCount >= resendLimit) {
            throw new BusinessException("RESEND_LIMIT_EXCEEDED", "Too many verification emails sent. Please try later");
        }
        
        // Invalidate old tokens
        tokenRepository.deleteByUserId(user.getId());
        
        // Generate new token
        String token = UUID.randomUUID().toString();
        
        VerificationToken verificationToken = new VerificationToken();
        verificationToken.setToken(token);
        verificationToken.setUser(user);
        verificationToken.setExpiryDate(LocalDateTime.now().plusHours(tokenExpiryHours));
        
        tokenRepository.save(verificationToken);
        
        // Send email async
        emailService.sendVerificationEmail(user.getEmail(), token);
    }
    
    @Transactional
    public void verifyEmail(String token) {
        VerificationToken verificationToken = tokenRepository.findByToken(token);
        
        // Validate token exists
        if (verificationToken == null) {
            throw new BusinessException("INVALID_TOKEN", "Invalid verification token");
        }
        
        // Validate not used
        if (verificationToken.getUsed()) {
            throw new BusinessException("TOKEN_ALREADY_USED", "Verification token already used");
        }
        
        // Validate not expired
        if (verificationToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new BusinessException("TOKEN_EXPIRED", "Verification token expired. Please request a new one");
        }
        
        User user = verificationToken.getUser();
        
        // Check if already verified
        if (user.getEmailVerified()) {
            throw new BusinessException("ALREADY_VERIFIED", "Email already verified");
        }
        
        // Check if user deleted
        if (user.getDeleted()) {
            throw new BusinessException("USER_DELETED", "User account is deleted");
        }
        
        // Activate user
        user.setEmailVerified(true);
        
        // Auto-approve PATIENT, others need admin approval
        if (user.getRole() == Role.PATIENT) {
            user.setApproved(true);
        }
        
        userRepository.save(user);
        
        // Mark token as used
        verificationToken.setUsed(true);
        tokenRepository.save(verificationToken);
        
        // Delete other tokens
        tokenRepository.deleteByUserId(user.getId());
    }
    
    @Transactional
    public void resendVerificationEmail(String email) {
        User user = userRepository.findByEmail(email);
        
        if (user == null || user.getDeleted()) {
            // Don't reveal if email exists (prevent enumeration)
            throw new BusinessException("EMAIL_SENT", "If the email exists, a verification link has been sent");
        }
        
        if (user.getEmailVerified()) {
            throw new BusinessException("ALREADY_VERIFIED", "Email already verified");
        }
        
        generateAndSendToken(user);
    }
}
