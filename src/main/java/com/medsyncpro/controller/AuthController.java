package com.medsyncpro.controller;

import com.medsyncpro.dto.request.LoginRequest;
import com.medsyncpro.dto.request.RegisterRequest;
import com.medsyncpro.dto.response.LoginResponse;
import com.medsyncpro.dto.response.RegisterResponse;
import com.medsyncpro.entity.RefreshToken;
import com.medsyncpro.entity.User;
import com.medsyncpro.exception.BusinessException;
import com.medsyncpro.response.ApiResponse;
import com.medsyncpro.service.*;
import com.medsyncpro.utils.Utils;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;


@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {
    private final UserService userService;
    private final VerificationService verificationService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final TokenBlacklistService tokenBlacklistService;
    private final Utils utils;
    private final AuthService authService;
    
    @Value("${jwt.access-expiration:900000}")
    private long accessExpiration;
    
    @Value("${jwt.refresh-expiration:604800000}")
    private long refreshExpiration;

    // ====================== ME ========================
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<User>> getCurrentUser(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(utils.getUserFromAuth(authentication), "Session Valid"));
    }

    
    // ==================== REGISTER ====================
    
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisterResponse>> register(@Valid @RequestBody RegisterRequest request) {
        RegisterResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Registration successful. Please check your email to verify your account"));
    }
    
    // ==================== LOGIN ====================
    
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse response) {
        
        ApiResponse<LoginResponse> loginResponse = authService.login(request, httpRequest, response);
        
        log.info("User {} logged in from device: {}", loginResponse.getData());
        return ResponseEntity.ok(loginResponse);
    }
    
    // ==================== REFRESH TOKEN ====================
    
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<LoginResponse>> refreshToken(
            HttpServletRequest request,
            HttpServletResponse response) {
        
        ApiResponse<LoginResponse> loginResponse = authService.refreshToken(request, response);
        return ResponseEntity.ok(loginResponse);
    }
    
    // ==================== LOGOUT (Current Device) ====================
    
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Object>> logout(
            HttpServletRequest request,
            HttpServletResponse response) {

        String accessToken = extractCookieValue(request, "access_token");
        String refreshToken = extractCookieValue(request, "refresh_token");

        // 1️⃣ Blacklist access token (if exists)
        if (accessToken != null) {
            try {
                Claims claims = jwtService.extractClaimsAllowExpired(accessToken);
                String jti = jwtService.extractJti(claims);
                Instant expiry = jwtService.extractExpiration(claims).toInstant();

                if (jti != null) {
                    tokenBlacklistService.blacklist(jti, expiry);
                }
            } catch (Exception e) {
                log.warn("Error blacklisting token: {}", e.getMessage());
            }
        }

        // 2️⃣ Revoke refresh token
        if (refreshToken != null) {
            refreshTokenService.revokeToken(refreshToken);
        }

        // 3️⃣ Clear cookies
        clearAccessTokenCookie(response);
        clearRefreshTokenCookie(response);

        return ResponseEntity.ok(
                ApiResponse.success(null, "Logged out successfully"));
    }
    
    // ==================== LOGOUT ALL DEVICES ====================
    
    @PostMapping("/logout-all")
    public ResponseEntity<ApiResponse<Object>> logoutAll(
            Authentication authentication,
            HttpServletRequest request,
            HttpServletResponse response) {

        String accessToken = extractCookieValue(request, "access_token");

        User user = null;

        if (authentication != null && authentication.getName() != null) {
            user = userService.getUserByEmail(authentication.getName());
        }

        if (user == null) {
            throw new BusinessException(
                    "UNAUTHORIZED",
                    "Unable to identify user. Please login again");
        }

        // 1️⃣ Blacklist current access token
        if (accessToken != null) {
            try {
                Claims claims = jwtService.extractClaimsAllowExpired(accessToken);
                String jti = jwtService.extractJti(claims);
                Instant expiry = jwtService.extractExpiration(claims).toInstant();

                if (jti != null) {
                    tokenBlacklistService.blacklist(jti, expiry);
                }
            } catch (Exception e) {
                log.warn("Error blacklisting token: {}", e.getMessage());
            }
        }

        // 2️⃣ Revoke ALL refresh tokens
        refreshTokenService.revokeAllUserTokens(user);

        // 3️⃣ Increment token version → instantly invalidates all access tokens
        userService.incrementTokenVersion(user);

        // 4️⃣ Clear cookies
        clearAccessTokenCookie(response);
        clearRefreshTokenCookie(response);

        return ResponseEntity.ok(
                ApiResponse.success(null, "Logged out from all devices successfully"));
    }
    
    // ==================== EMAIL VERIFICATION ====================
    
    @PostMapping("/verify-email")
    public ResponseEntity<ApiResponse<Object>> verifyEmail(@Valid @RequestBody com.medsyncpro.dto.request.VerifyEmailRequest request) {
        verificationService.verifyEmail(request.getToken());
        return ResponseEntity.ok(ApiResponse.success(null, "Email verified successfully"));
    }
    
    @PostMapping("/resend-verification")
    public ResponseEntity<ApiResponse<Object>> resendVerification(@RequestParam String email) {
        verificationService.resendVerificationEmail(email);
        return ResponseEntity.ok(ApiResponse.success(null, "Verification email sent"));
    }
    
    // ==================== HELPERS ====================
    
    private String extractCookieValue(HttpServletRequest request, String cookieName) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (cookieName.equals(cookie.getName())) {
                    String value = cookie.getValue();
                    return (value != null && !value.isBlank()) ? value : null;
                }
            }
        }
        return null;
    }
    
    private void addAccessTokenCookie(HttpServletResponse response, String token) {
        org.springframework.http.ResponseCookie cookie = org.springframework.http.ResponseCookie.from("access_token", token)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(accessExpiration / 1000)
                .sameSite("None")
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }
    
    private void addRefreshTokenCookie(HttpServletResponse response, String token) {
        org.springframework.http.ResponseCookie cookie = org.springframework.http.ResponseCookie.from("refresh_token", token)
                .httpOnly(true)
                .secure(true)
                .path("/api/auth")
                .maxAge(refreshExpiration / 1000)
                .sameSite("None")
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }
    
    private void clearAccessTokenCookie(HttpServletResponse response) {
        org.springframework.http.ResponseCookie cookie = org.springframework.http.ResponseCookie.from("access_token", "")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(0)
                .sameSite("None")
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }
    
    private void clearRefreshTokenCookie(HttpServletResponse response) {
        org.springframework.http.ResponseCookie cookie = org.springframework.http.ResponseCookie.from("refresh_token", "")
                .httpOnly(true)
                .secure(true)
                .path("/api/auth")
                .maxAge(0)
                .sameSite("None")
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }
}
