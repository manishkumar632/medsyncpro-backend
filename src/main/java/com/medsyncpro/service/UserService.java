package com.medsyncpro.service;

import com.medsyncpro.dto.RegisterRequest;
import com.medsyncpro.entity.Role;
import com.medsyncpro.entity.User;
import com.medsyncpro.exception.BusinessException;
import com.medsyncpro.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import org.springframework.context.ApplicationEventPublisher;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final VerificationService verificationService;
    private final ApplicationEventPublisher eventPublisher;
    
    @Value("${admin.secret:}")
    private String adminSecret;
    
    @Value("${registration.spam.limit:3}")
    private int spamLimit;
    
    @Transactional
    public User register(RegisterRequest request) {
        // Password match validation
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException("PASSWORD_MISMATCH", "password and confirm password are not same");
        }
        
        // Admin role restriction
        if (request.getRole() == Role.ADMIN) {
            throw new BusinessException("ADMIN_REGISTRATION_DISABLED", "Admin registration is not allowed");
        }

        if (request.getTermsAccepted() == false) {
            throw new BusinessException("TERMS_AND_CONDITION_NOT_ACCEPTED", "Please accept terms and condition");
        }
        
        // Spam detection
        long recentCount = userRepository.countRecentRegistrationsByEmail(
            request.getEmail(), 
            LocalDateTime.now().minusHours(1)
        );
        if (recentCount >= spamLimit) {
            throw new BusinessException("SPAM_DETECTED", "Too many registration attempts. Please try later");
        }
        
        // Check existing active users
        if (userRepository.existsByEmailAndNotDeleted(request.getEmail())) {
            throw new BusinessException("EMAIL_EXISTS", "User with the email already exist. Please login");
        }
        
        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole());
        
        // All users default unverified professional status
        user.setProfessionalVerificationStatus(com.medsyncpro.entity.VerificationStatus.UNVERIFIED);
        
        try {
            user = userRepository.save(user);
            
            // Generate and send verification token
            verificationService.generateAndSendToken(user);
            
            // Trigger verification workflow for DOCTOR and PHARMACIST
            if (user.getRole() == Role.DOCTOR || user.getRole() == Role.PHARMACIST) {
                eventPublisher.publishEvent(new com.medsyncpro.event.UserSignupEvent(this, user));
            }
            
            return user;
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException("DUPLICATE_ENTRY", "Email or username already exists");
        }
    }
    
    public com.medsyncpro.dto.LoginResponse login(com.medsyncpro.dto.LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail());
        
        if (user == null || user.getDeleted() || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Invalid credentials");
        }
        
        if (!user.getEmailVerified()) {
            throw new BusinessException("EMAIL_NOT_VERIFIED", "Please verify your email before logging in");
        }
        
        return new com.medsyncpro.dto.LoginResponse(user.getId(), user.getEmail(), user.getName(), user.getRole(), user.getProfessionalVerificationStatus());
    }
    
    public String generateToken(User user) {
        return jwtService.generateAccessToken(user, "unknown");
    }
    
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }
    
    @Transactional
    public void incrementTokenVersion(User user) {
        user.setTokenVersion(user.getTokenVersion() + 1);
        userRepository.save(user);
    }
}
