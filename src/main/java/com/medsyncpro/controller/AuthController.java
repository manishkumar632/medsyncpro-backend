package com.medsyncpro.controller;

import com.medsyncpro.dto.LoginRequest;
import com.medsyncpro.dto.LoginResponse;
import com.medsyncpro.dto.RegisterRequest;
import com.medsyncpro.entity.User;
import com.medsyncpro.response.ApiResponse;
import com.medsyncpro.service.UserService;
import com.medsyncpro.service.VerificationService;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final UserService userService;
    private final VerificationService verificationService;
    
    @Value("${jwt.expiration:86400000}")
    private long jwtExpiration;
    
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<User>> register(@Valid @RequestBody RegisterRequest request) {
        User user = userService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(user, "User registered successfully. Please verify your email"));
    }
    
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        LoginResponse loginResponse = userService.login(request);
        
        // Get user and generate token
        User user = userService.getUserByEmail(request.getEmail());
        String token = userService.generateToken(user);
        
        // Create HttpOnly Secure Cookie
        Cookie cookie = new Cookie("token", token);
        cookie.setHttpOnly(true);
        cookie.setSecure(true); // Only over HTTPS
        cookie.setPath("/");
        cookie.setMaxAge((int) (jwtExpiration / 1000)); // Convert ms to seconds
        
        response.addCookie(cookie);
        
        return ResponseEntity.ok(ApiResponse.success(loginResponse, "Login successful"));
    }
    
    @PostMapping("/verify-email")
    public ResponseEntity<ApiResponse<Object>> verifyEmail(@Valid @RequestBody com.medsyncpro.dto.VerifyEmailRequest request) {
        verificationService.verifyEmail(request.getToken());
        return ResponseEntity.ok(ApiResponse.success(null, "Email verified successfully"));
    }
    
    @PostMapping("/resend-verification")
    public ResponseEntity<ApiResponse<Object>> resendVerification(@RequestParam String email) {
        verificationService.resendVerificationEmail(email);
        return ResponseEntity.ok(ApiResponse.success(null, "Verification email sent"));
    }
    
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Object>> logout(HttpServletResponse response) {
        Cookie cookie = new Cookie("token", null);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        
        response.addCookie(cookie);
        
        return ResponseEntity.ok(ApiResponse.success(null, "Logged out successfully"));
    }
}
